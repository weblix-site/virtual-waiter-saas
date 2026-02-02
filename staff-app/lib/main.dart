import 'dart:convert';
import 'dart:async';
import 'dart:math';

import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:audioplayers/audioplayers.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';

@pragma('vm:entry-point')
Future<void> _firebaseMessagingBackgroundHandler(RemoteMessage message) async {
  await Firebase.initializeApp();
}

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await Firebase.initializeApp();
  FirebaseMessaging.onBackgroundMessage(_firebaseMessagingBackgroundHandler);
  runApp(const StaffApp());
}

const String apiBase = String.fromEnvironment('API_BASE', defaultValue: 'http://localhost:8080');

const int slaOrderWarnMin = 5;
const int slaOrderCritMin = 10;
const int slaCallWarnMin = 2;
const int slaCallCritMin = 5;
const int slaBillWarnMin = 5;
const int slaBillCritMin = 10;
const int slaKitchenWarnMin = 7;
const int slaKitchenCritMin = 15;

DateTime? _parseIso(String? value) {
  if (value == null || value.isEmpty) return null;
  try {
    return DateTime.parse(value).toLocal();
  } catch (_) {
    return null;
  }
}

Duration _ageFromIso(String? value) {
  final dt = _parseIso(value);
  if (dt == null) return Duration.zero;
  final now = DateTime.now();
  return now.difference(dt).isNegative ? Duration.zero : now.difference(dt);
}

Color _slaColor(Duration age, int warnMin, int critMin) {
  final minutes = age.inMinutes;
  if (minutes >= critMin) return Colors.red.shade600;
  if (minutes >= warnMin) return Colors.orange.shade600;
  return Colors.green.shade600;
}

Widget _slaChip(Duration age, int warnMin, int critMin) {
  final minutes = age.inMinutes;
  final label = minutes < 1 ? '<1m' : '${minutes}m';
  final color = _slaColor(age, warnMin, critMin);
  return Container(
    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
    decoration: BoxDecoration(
      color: color.withOpacity(0.1),
      borderRadius: BorderRadius.circular(999),
      border: Border.all(color: color, width: 1),
    ),
    child: Text(
      label,
      style: TextStyle(color: color, fontWeight: FontWeight.w600),
    ),
  );
}

Widget _slaSummaryRow(int warn, int crit) {
  return Row(
    children: [
      Container(
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
        decoration: BoxDecoration(
          color: Colors.orange.withOpacity(0.1),
          borderRadius: BorderRadius.circular(999),
          border: Border.all(color: Colors.orange, width: 1),
        ),
        child: Text('Warn: $warn', style: const TextStyle(color: Colors.orange, fontWeight: FontWeight.w600)),
      ),
      const SizedBox(width: 8),
      Container(
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
        decoration: BoxDecoration(
          color: Colors.red.withOpacity(0.1),
          borderRadius: BorderRadius.circular(999),
          border: Border.all(color: Colors.red, width: 1),
        ),
        child: Text('Crit: $crit', style: const TextStyle(color: Colors.red, fontWeight: FontWeight.w600)),
      ),
    ],
  );
}
class StaffApp extends StatelessWidget {
  const StaffApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Virtual Waiter - Staff',
      theme: ThemeData(useMaterial3: true),
      home: const LoginScreen(),
    );
  }
}

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _user = TextEditingController(text: 'waiter1');
  final _pass = TextEditingController(text: 'demo123');
  bool _loading = false;
  String? _error;

  Future<void> _login() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final auth = base64Encode(utf8.encode('${_user.text}:${_pass.text}'));
      final res = await http.get(
        Uri.parse('$apiBase/api/staff/me'),
        headers: {'Authorization': 'Basic $auth'},
      );
      if (res.statusCode != 200) {
        throw Exception('Login failed (${res.statusCode})');
      }
      if (!mounted) return;
      Navigator.of(context).pushReplacement(
        MaterialPageRoute(builder: (_) => HomeScreen(username: _user.text, password: _pass.text)),
      );
    } catch (e) {
      setState(() => _error = e.toString());
    } finally {
      setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Staff Login')),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            TextField(controller: _user, decoration: const InputDecoration(labelText: 'Username')),
            const SizedBox(height: 8),
            TextField(controller: _pass, decoration: const InputDecoration(labelText: 'Password'), obscureText: true),
            const SizedBox(height: 16),
            if (_error != null) Text(_error!, style: const TextStyle(color: Colors.red)),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                onPressed: _loading ? null : _login,
                child: Text(_loading ? '...' : 'Login'),
              ),
            ),
            const SizedBox(height: 12),
            const Text('Demo credentials: waiter1 / demo123'),
          ],
        ),
      ),
    );
  }
}

class HomeScreen extends StatefulWidget {
  final String username;
  final String password;

  const HomeScreen({super.key, required this.username, required this.password});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  int _index = 0;
  Timer? _timer;
  DateTime _sinceOrders = DateTime.now();
  DateTime _sinceCalls = DateTime.now();
  DateTime _sinceBills = DateTime.now();
  int _newOrders = 0;
  int _newCalls = 0;
  int _newBills = 0;
  int _lastNotifId = 0;
  final List<Map<String, dynamic>> _events = [];
  String? _deviceToken;
  final AudioPlayer _player = AudioPlayer();
  DateTime? _lastKitchenBeepAt;
  DateTime? _lastSnackAt;

  String get _auth => base64Encode(utf8.encode('${widget.username}:${widget.password}'));

  Future<void> _poll() async {
    try {
      final feed = await http.get(
        Uri.parse('$apiBase/api/staff/notifications/feed?sinceId=$_lastNotifId'),
        headers: {'Authorization': 'Basic $_auth'},
      );
      if (feed.statusCode == 200) {
        final body = jsonDecode(feed.body);
        final events = (body['events'] as List<dynamic>? ?? const []).cast<Map<String, dynamic>>();
        if (events.isNotEmpty) {
          setState(() {
            _lastNotifId = body['lastId'] ?? _lastNotifId;
            _events.insertAll(0, events);
          });

          // Play sound for new orders (kitchen)
          final hasNewOrder = events.any((e) => e['type'] == 'ORDER_NEW');
          if (hasNewOrder) {
            final now = DateTime.now();
            if (_lastKitchenBeepAt == null || now.difference(_lastKitchenBeepAt!) > const Duration(seconds: 10)) {
              _lastKitchenBeepAt = now;
              // System beep as fallback
              // For simplicity use a short system sound
              _player.play(AssetSource('beep.wav'));
            }
          }

          if (mounted) {
            final now = DateTime.now();
            if (_lastSnackAt == null || now.difference(_lastSnackAt!) > const Duration(seconds: 10)) {
              _lastSnackAt = now;
              final orders = events.where((e) => e['type'] == 'ORDER_NEW').length;
              final calls = events.where((e) => e['type'] == 'WAITER_CALL').length;
              final bills = events.where((e) => e['type'] == 'BILL_REQUEST').length;
              final parts = <String>[];
              if (orders > 0) parts.add('Orders: $orders');
              if (calls > 0) parts.add('Calls: $calls');
              if (bills > 0) parts.add('Bills: $bills');
              if (parts.isNotEmpty) {
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(content: Text('New events • ${parts.join("  ")}')),
                );
              }
            }
          }
        }
      }
      final res = await http.get(
        Uri.parse('$apiBase/api/staff/notifications?sinceOrders=${_sinceOrders.toUtc().toIso8601String()}&sinceCalls=${_sinceCalls.toUtc().toIso8601String()}&sinceBills=${_sinceBills.toUtc().toIso8601String()}'),
        headers: {'Authorization': 'Basic $_auth'},
      );
      if (res.statusCode != 200) return;
      final body = jsonDecode(res.body);
      setState(() {
        _newOrders = body['newOrders'] ?? 0;
        _newCalls = body['newCalls'] ?? 0;
        _newBills = body['newBills'] ?? 0;
      });
    } catch (_) {}
  }

  Future<void> _registerDevice() async {
    await _registerFcmToken();
    if (_deviceToken == null) {
      _deviceToken = _genToken();
    }
    try {
      await http.post(
        Uri.parse('$apiBase/api/staff/devices/register'),
        headers: {'Authorization': 'Basic $_auth', 'Content-Type': 'application/json'},
        body: jsonEncode({'token': _deviceToken, 'platform': 'APP'}),
      );
    } catch (_) {}
  }

  Future<void> _registerFcmToken() async {
    try {
      final fcm = FirebaseMessaging.instance;
      await fcm.requestPermission();
      final token = await fcm.getToken();
      if (token == null || token.isEmpty) return;
      _deviceToken = token;
      await http.post(
        Uri.parse('$apiBase/api/staff/devices/register'),
        headers: {'Authorization': 'Basic $_auth', 'Content-Type': 'application/json'},
        body: jsonEncode({'token': token, 'platform': 'FCM'}),
      );
    } catch (_) {}
  }
  String _genToken() {
    const chars = 'abcdefghijklmnopqrstuvwxyz0123456789';
    final rnd = Random();
    return List.generate(32, (_) => chars[rnd.nextInt(chars.length)]).join();
  }

  @override
  void initState() {
    super.initState();
    _listenToFcm();
    _timer = Timer.periodic(const Duration(seconds: 15), (_) => _poll());
    _poll();
    _registerDevice();
  }

  void _listenToFcm() {
    FirebaseMessaging.onMessage.listen((message) {
      // Surface push in UI and play sound if needed.
      final type = message.data['type']?.toString();
      if (type == 'ORDER_NEW') {
        _player.play(AssetSource('beep.wav'));
      }
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Push: ${message.data['type'] ?? message.notification?.title ?? 'Notification'}')),
        );
      }
    });
    FirebaseMessaging.instance.onTokenRefresh.listen((token) async {
      _deviceToken = token;
      try {
        await http.post(
          Uri.parse('$apiBase/api/staff/devices/register'),
          headers: {'Authorization': 'Basic $_auth', 'Content-Type': 'application/json'},
          body: jsonEncode({'token': token, 'platform': 'FCM'}),
        );
      } catch (_) {}
    });
  }

  @override
  void dispose() {
    _timer?.cancel();
    _player.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final tabs = [
      OrdersTab(username: widget.username, password: widget.password),
      CallsTab(username: widget.username, password: widget.password),
      BillsTab(username: widget.username, password: widget.password),
      KitchenTab(username: widget.username, password: widget.password),
      FloorPlanTab(username: widget.username, password: widget.password),
      HistoryTab(username: widget.username, password: widget.password),
      NotificationsTab(events: _events),
    ];
    return Scaffold(
      body: tabs[_index],
      bottomNavigationBar: NavigationBar(
        selectedIndex: _index,
        onDestinationSelected: (i) {
          setState(() {
            _index = i;
            final now = DateTime.now();
            if (i == 0) {
              _sinceOrders = now;
              _newOrders = 0;
            } else if (i == 1) {
              _sinceCalls = now;
              _newCalls = 0;
            } else if (i == 2) {
              _sinceBills = now;
              _newBills = 0;
            }
          });
        },
        destinations: [
          NavigationDestination(
            icon: _newOrders > 0 ? Badge(label: Text('$_newOrders'), child: const Icon(Icons.receipt_long)) : const Icon(Icons.receipt_long),
            label: 'Orders',
          ),
          NavigationDestination(
            icon: _newCalls > 0 ? Badge(label: Text('$_newCalls'), child: const Icon(Icons.notifications_active)) : const Icon(Icons.notifications_active),
            label: 'Calls',
          ),
          NavigationDestination(
            icon: _newBills > 0 ? Badge(label: Text('$_newBills'), child: const Icon(Icons.payments)) : const Icon(Icons.payments),
            label: 'Bills',
          ),
          const NavigationDestination(icon: Icon(Icons.kitchen), label: 'Kitchen'),
          const NavigationDestination(icon: Icon(Icons.map), label: 'Hall'),
          const NavigationDestination(icon: Icon(Icons.history), label: 'History'),
          const NavigationDestination(icon: Icon(Icons.notifications), label: 'Events'),
        ],
      ),
    );
  }
}

class NotificationsTab extends StatelessWidget {
  final List<Map<String, dynamic>> events;

  const NotificationsTab({super.key, required this.events});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Events')),
      body: events.isEmpty
          ? const Center(child: Text('No events yet'))
          : ListView.separated(
              itemCount: events.length,
              separatorBuilder: (_, __) => const Divider(height: 1),
              itemBuilder: (ctx, i) {
                final e = events[i];
                return ListTile(
                  title: Text('${e['type']} • #${e['refId']}'),
                  subtitle: Text('${e['createdAt']}'),
                );
              },
            ),
    );
  }
}

class OrdersTab extends StatefulWidget {
  final String username;
  final String password;

  const OrdersTab({super.key, required this.username, required this.password});

  @override
  State<OrdersTab> createState() => _OrdersTabState();
}

class _OrdersTabState extends State<OrdersTab> {
  bool _loading = true;
  String? _error;
  List<dynamic> _orders = const [];
  String _sortMode = 'time_desc';
  bool _showFocus = true;

  String get _auth => base64Encode(utf8.encode('${widget.username}:${widget.password}'));

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final res = await http.get(
        Uri.parse('$apiBase/api/staff/orders/active'),
        headers: {'Authorization': 'Basic $_auth'},
      );
      if (res.statusCode != 200) throw Exception('Load failed (${res.statusCode})');
      final body = jsonDecode(res.body);
      setState(() => _orders = body as List<dynamic>);
    } catch (e) {
      setState(() => _error = e.toString());
    } finally {
      setState(() => _loading = false);
    }
  }

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Active Orders'),
        actions: [
          IconButton(
            onPressed: () => setState(() => _showFocus = !_showFocus),
            icon: Icon(_showFocus ? Icons.visibility : Icons.visibility_off),
          ),
          PopupMenuButton<String>(
            onSelected: (v) => setState(() => _sortMode = v),
            itemBuilder: (ctx) => const [
              PopupMenuItem(value: 'time_desc', child: Text('Sort: newest first')),
              PopupMenuItem(value: 'sla_desc', child: Text('Sort: SLA (oldest first)')),
            ],
          ),
          IconButton(onPressed: _load, icon: const Icon(Icons.refresh)),
        ],
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(child: Text(_error!))
              : Builder(builder: (context) {
                  final orders = _orders.toList();
                  orders.sort((a, b) {
                    if (_sortMode == 'sla_desc') {
                      final aa = _ageFromIso(a['createdAt']?.toString()).inSeconds;
                      final bb = _ageFromIso(b['createdAt']?.toString()).inSeconds;
                      return bb.compareTo(aa);
                    }
                    return (b['createdAt'] ?? '').toString().compareTo((a['createdAt'] ?? '').toString());
                  });
                  int warn = 0;
                  int crit = 0;
                  final Map<int, Duration> maxAgeByTable = {};
                  final Map<int, int> countByTable = {};
                  for (final o in orders) {
                    final age = _ageFromIso(o['createdAt']?.toString());
                    final tableNumber = (o['tableNumber'] ?? 0) as int;
                    final prev = maxAgeByTable[tableNumber];
                    if (prev == null || age > prev) {
                      maxAgeByTable[tableNumber] = age;
                    }
                    countByTable[tableNumber] = (countByTable[tableNumber] ?? 0) + 1;
                    if (age.inMinutes >= slaOrderCritMin) crit++;
                    else if (age.inMinutes >= slaOrderWarnMin) warn++;
                  }
                  final focusTables = maxAgeByTable.entries.toList()
                    ..sort((a, b) => b.value.inSeconds.compareTo(a.value.inSeconds));
                  final topFocus = focusTables.take(3).toList();
                  return Column(
                    children: [
                      Padding(
                        padding: const EdgeInsets.fromLTRB(16, 12, 16, 4),
                        child: _slaSummaryRow(warn, crit),
                      ),
                      if (_showFocus && topFocus.isNotEmpty)
                        SizedBox(
                          height: 96,
                          child: ListView.separated(
                            padding: const EdgeInsets.fromLTRB(16, 8, 16, 8),
                            scrollDirection: Axis.horizontal,
                            itemCount: topFocus.length,
                            separatorBuilder: (_, __) => const SizedBox(width: 10),
                            itemBuilder: (ctx, i) {
                              final entry = topFocus[i];
                              final tableNo = entry.key;
                              final age = entry.value;
                              final count = countByTable[tableNo] ?? 0;
                              final color = _slaColor(age, slaOrderWarnMin, slaOrderCritMin);
                              final minutes = age.inMinutes < 1 ? '<1m' : '${age.inMinutes}m';
                              return Container(
                                width: 180,
                                padding: const EdgeInsets.all(12),
                                decoration: BoxDecoration(
                                  color: color.withOpacity(0.08),
                                  borderRadius: BorderRadius.circular(12),
                                  border: Border.all(color: color.withOpacity(0.4)),
                                ),
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  mainAxisAlignment: MainAxisAlignment.center,
                                  children: [
                                    Text('Table #$tableNo', style: const TextStyle(fontWeight: FontWeight.w700)),
                                    const SizedBox(height: 6),
                                    Text('Oldest: $minutes', style: TextStyle(color: color, fontWeight: FontWeight.w600)),
                                    const SizedBox(height: 4),
                                    Text('Orders: $count', style: const TextStyle(color: Colors.black54)),
                                  ],
                                ),
                              );
                            },
                          ),
                        ),
                      Expanded(
                        child: ListView.separated(
                          itemCount: orders.length,
                          separatorBuilder: (_, __) => const Divider(height: 1),
                          itemBuilder: (ctx, i) {
                            final o = orders[i] as Map<String, dynamic>;
                    final items = (o['items'] as List<dynamic>? ?? const []).cast<Map<String, dynamic>>();
                    final tableNumber = o['tableNumber'];
                    final assigned = o['assignedWaiterId'];
                    final age = _ageFromIso(o['createdAt']?.toString());
                    return ListTile(
                      title: Text('Table #$tableNumber  •  Order #${o['id']}'),
                      subtitle: Text('${o['status']} • ${items.length} item(s)' + (assigned != null ? ' • waiter #$assigned' : '')),
                      trailing: _slaChip(age, slaOrderWarnMin, slaOrderCritMin),
                      onTap: () {
                        Navigator.of(context).push(MaterialPageRoute(
                          builder: (_) => OrderDetailsScreen(
                            order: o,
                            username: widget.username,
                            password: widget.password,
                            actions: const ['ACCEPTED', 'IN_PROGRESS', 'READY'],
                          ),
                        ));
                      },
                    );
                          },
                        ),
                      ),
                    ],
                  );
                }),
    );
  }
}

class CallsTab extends StatefulWidget {
  final String username;
  final String password;

  const CallsTab({super.key, required this.username, required this.password});

  @override
  State<CallsTab> createState() => _CallsTabState();
}

class _CallsTabState extends State<CallsTab> {
  bool _loading = true;
  String? _error;
  List<dynamic> _calls = const [];
  String _sortMode = 'time_desc';
  bool _showFocus = true;

  String get _auth => base64Encode(utf8.encode('${widget.username}:${widget.password}'));

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final res = await http.get(
        Uri.parse('$apiBase/api/staff/waiter-calls/active'),
        headers: {'Authorization': 'Basic $_auth'},
      );
      if (res.statusCode != 200) throw Exception('Load failed (${res.statusCode})');
      final body = jsonDecode(res.body);
      setState(() => _calls = body as List<dynamic>);
    } catch (e) {
      setState(() => _error = e.toString());
    } finally {
      setState(() => _loading = false);
    }
  }

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Waiter Calls'),
        actions: [
          IconButton(
            onPressed: () => setState(() => _showFocus = !_showFocus),
            icon: Icon(_showFocus ? Icons.visibility : Icons.visibility_off),
          ),
          PopupMenuButton<String>(
            onSelected: (v) => setState(() => _sortMode = v),
            itemBuilder: (ctx) => const [
              PopupMenuItem(value: 'time_desc', child: Text('Sort: newest first')),
              PopupMenuItem(value: 'sla_desc', child: Text('Sort: SLA (oldest first)')),
            ],
          ),
          IconButton(onPressed: _load, icon: const Icon(Icons.refresh)),
        ],
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(child: Text(_error!))
              : Builder(builder: (context) {
                  final calls = _calls.toList();
                  calls.sort((a, b) {
                    if (_sortMode == 'sla_desc') {
                      final aa = _ageFromIso(a['createdAt']?.toString()).inSeconds;
                      final bb = _ageFromIso(b['createdAt']?.toString()).inSeconds;
                      return bb.compareTo(aa);
                    }
                    return (b['createdAt'] ?? '').toString().compareTo((a['createdAt'] ?? '').toString());
                  });
                  int warn = 0;
                  int crit = 0;
                  final Map<int, Duration> maxAgeByTable = {};
                  final Map<int, int> countByTable = {};
                  for (final c in calls) {
                    final age = _ageFromIso(c['createdAt']?.toString());
                    final tableNumber = (c['tableNumber'] ?? 0) as int;
                    final prev = maxAgeByTable[tableNumber];
                    if (prev == null || age > prev) {
                      maxAgeByTable[tableNumber] = age;
                    }
                    countByTable[tableNumber] = (countByTable[tableNumber] ?? 0) + 1;
                    if (age.inMinutes >= slaCallCritMin) crit++;
                    else if (age.inMinutes >= slaCallWarnMin) warn++;
                  }
                  final focusTables = maxAgeByTable.entries.toList()
                    ..sort((a, b) => b.value.inSeconds.compareTo(a.value.inSeconds));
                  final topFocus = focusTables.take(3).toList();
                  return Column(
                    children: [
                      Padding(
                        padding: const EdgeInsets.fromLTRB(16, 12, 16, 4),
                        child: _slaSummaryRow(warn, crit),
                      ),
                      if (_showFocus && topFocus.isNotEmpty)
                        SizedBox(
                          height: 96,
                          child: ListView.separated(
                            padding: const EdgeInsets.fromLTRB(16, 8, 16, 8),
                            scrollDirection: Axis.horizontal,
                            itemCount: topFocus.length,
                            separatorBuilder: (_, __) => const SizedBox(width: 10),
                            itemBuilder: (ctx, i) {
                              final entry = topFocus[i];
                              final tableNo = entry.key;
                              final age = entry.value;
                              final count = countByTable[tableNo] ?? 0;
                              final color = _slaColor(age, slaCallWarnMin, slaCallCritMin);
                              final minutes = age.inMinutes < 1 ? '<1m' : '${age.inMinutes}m';
                              return Container(
                                width: 180,
                                padding: const EdgeInsets.all(12),
                                decoration: BoxDecoration(
                                  color: color.withOpacity(0.08),
                                  borderRadius: BorderRadius.circular(12),
                                  border: Border.all(color: color.withOpacity(0.4)),
                                ),
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  mainAxisAlignment: MainAxisAlignment.center,
                                  children: [
                                    Text('Table #$tableNo', style: const TextStyle(fontWeight: FontWeight.w700)),
                                    const SizedBox(height: 6),
                                    Text('Oldest: $minutes', style: TextStyle(color: color, fontWeight: FontWeight.w600)),
                                    const SizedBox(height: 4),
                                    Text('Calls: $count', style: const TextStyle(color: Colors.black54)),
                                  ],
                                ),
                              );
                            },
                          ),
                        ),
                      Expanded(
                        child: ListView.separated(
                          itemCount: calls.length,
                          separatorBuilder: (_, __) => const Divider(height: 1),
                          itemBuilder: (ctx, i) {
                            final c = calls[i] as Map<String, dynamic>;
                            final age = _ageFromIso(c['createdAt']?.toString());
                            return ListTile(
                              title: Text('Table #${c['tableNumber']}'),
                              subtitle: Text('${c['status']} • ${c['createdAt']}'),
                              leading: const Icon(Icons.notifications_active),
                              trailing: _slaChip(age, slaCallWarnMin, slaCallCritMin),
                              onTap: () async {
                                final changed = await Navigator.of(context).push(MaterialPageRoute(
                                  builder: (_) => WaiterCallDetailsScreen(
                                    call: c,
                                    username: widget.username,
                                    password: widget.password,
                                  ),
                                ));
                                if (changed == true && mounted) {
                                  _load();
                                }
                              },
                            );
                          },
                        ),
                      ),
                    ],
                  );
                }),
    );
  }
}

class WaiterCallDetailsScreen extends StatelessWidget {
  final Map<String, dynamic> call;
  final String username;
  final String password;

  const WaiterCallDetailsScreen({
    super.key,
    required this.call,
    required this.username,
    required this.password,
  });

  String get _auth => base64Encode(utf8.encode('$username:$password'));

  Future<void> _setStatus(BuildContext context, String status) async {
    final id = call['id'];
    final res = await http.post(
      Uri.parse('$apiBase/api/staff/waiter-calls/$id/status'),
      headers: {'Authorization': 'Basic $_auth', 'Content-Type': 'application/json'},
      body: jsonEncode({'status': status}),
    );
    if (res.statusCode >= 300) {
      if (!context.mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Status update failed (${res.statusCode})')));
      return;
    }
    if (!context.mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Status -> $status')));
    Navigator.of(context).pop(true);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('Call #${call['id']}')),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Table #${call['tableNumber']}', style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 8),
            Text('Status: ${call['status']}'),
            const SizedBox(height: 16),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton(
                    onPressed: () => _setStatus(context, 'ACKNOWLEDGED'),
                    child: const Text('Acknowledge'),
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: OutlinedButton(
                    onPressed: () => _setStatus(context, 'CLOSED'),
                    child: const Text('Close'),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class BillsTab extends StatefulWidget {
  final String username;
  final String password;

  const BillsTab({super.key, required this.username, required this.password});

  @override
  State<BillsTab> createState() => _BillsTabState();
}

class _BillsTabState extends State<BillsTab> {
  bool _loading = true;
  String? _error;
  List<dynamic> _bills = const [];
  String _sortMode = 'time_desc';
  bool _showFocus = true;

  String get _auth => base64Encode(utf8.encode('${widget.username}:${widget.password}'));

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final res = await http.get(
        Uri.parse('$apiBase/api/staff/bill-requests/active'),
        headers: {'Authorization': 'Basic $_auth'},
      );
      if (res.statusCode != 200) throw Exception('Load failed (${res.statusCode})');
      final body = jsonDecode(res.body);
      setState(() => _bills = body as List<dynamic>);
    } catch (e) {
      setState(() => _error = e.toString());
    } finally {
      setState(() => _loading = false);
    }
  }

  Future<void> _confirmPaid(int id) async {
    final res = await http.post(
      Uri.parse('$apiBase/api/staff/bill-requests/$id/confirm-paid'),
      headers: {'Authorization': 'Basic $_auth'},
    );
    if (res.statusCode >= 300) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Confirm failed (${res.statusCode})')));
      return;
    }
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Payment confirmed')));
    _load();
  }

  Future<void> _cancelBill(int id) async {
    final res = await http.post(
      Uri.parse('$apiBase/api/staff/bill-requests/$id/cancel'),
      headers: {'Authorization': 'Basic $_auth'},
    );
    if (res.statusCode >= 300) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Cancel failed (${res.statusCode})')));
      return;
    }
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Bill cancelled')));
    _load();
  }

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Bill Requests'),
        actions: [
          IconButton(
            onPressed: () => setState(() => _showFocus = !_showFocus),
            icon: Icon(_showFocus ? Icons.visibility : Icons.visibility_off),
          ),
          PopupMenuButton<String>(
            onSelected: (v) => setState(() => _sortMode = v),
            itemBuilder: (ctx) => const [
              PopupMenuItem(value: 'time_desc', child: Text('Sort: newest first')),
              PopupMenuItem(value: 'sla_desc', child: Text('Sort: SLA (oldest first)')),
            ],
          ),
          IconButton(onPressed: _load, icon: const Icon(Icons.refresh)),
        ],
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(child: Text(_error!))
              : Builder(builder: (context) {
                  final bills = _bills.toList();
                  bills.sort((a, b) {
                    if (_sortMode == 'sla_desc') {
                      final aa = _ageFromIso(a['createdAt']?.toString()).inSeconds;
                      final bb = _ageFromIso(b['createdAt']?.toString()).inSeconds;
                      return bb.compareTo(aa);
                    }
                    return (b['createdAt'] ?? '').toString().compareTo((a['createdAt'] ?? '').toString());
                  });
                  int warn = 0;
                  int crit = 0;
                  final Map<int, Duration> maxAgeByTable = {};
                  final Map<int, int> countByTable = {};
                  for (final b in bills) {
                    final age = _ageFromIso(b['createdAt']?.toString());
                    final tableNumber = (b['tableNumber'] ?? 0) as int;
                    final prev = maxAgeByTable[tableNumber];
                    if (prev == null || age > prev) {
                      maxAgeByTable[tableNumber] = age;
                    }
                    countByTable[tableNumber] = (countByTable[tableNumber] ?? 0) + 1;
                    if (age.inMinutes >= slaBillCritMin) crit++;
                    else if (age.inMinutes >= slaBillWarnMin) warn++;
                  }
                  final focusTables = maxAgeByTable.entries.toList()
                    ..sort((a, b) => b.value.inSeconds.compareTo(a.value.inSeconds));
                  final topFocus = focusTables.take(3).toList();
                  return Column(
                    children: [
                      Padding(
                        padding: const EdgeInsets.fromLTRB(16, 12, 16, 4),
                        child: _slaSummaryRow(warn, crit),
                      ),
                      if (_showFocus && topFocus.isNotEmpty)
                        SizedBox(
                          height: 96,
                          child: ListView.separated(
                            padding: const EdgeInsets.fromLTRB(16, 8, 16, 8),
                            scrollDirection: Axis.horizontal,
                            itemCount: topFocus.length,
                            separatorBuilder: (_, __) => const SizedBox(width: 10),
                            itemBuilder: (ctx, i) {
                              final entry = topFocus[i];
                              final tableNo = entry.key;
                              final age = entry.value;
                              final count = countByTable[tableNo] ?? 0;
                              final color = _slaColor(age, slaBillWarnMin, slaBillCritMin);
                              final minutes = age.inMinutes < 1 ? '<1m' : '${age.inMinutes}m';
                              return Container(
                                width: 180,
                                padding: const EdgeInsets.all(12),
                                decoration: BoxDecoration(
                                  color: color.withOpacity(0.08),
                                  borderRadius: BorderRadius.circular(12),
                                  border: Border.all(color: color.withOpacity(0.4)),
                                ),
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  mainAxisAlignment: MainAxisAlignment.center,
                                  children: [
                                    Text('Table #$tableNo', style: const TextStyle(fontWeight: FontWeight.w700)),
                                    const SizedBox(height: 6),
                                    Text('Oldest: $minutes', style: TextStyle(color: color, fontWeight: FontWeight.w600)),
                                    const SizedBox(height: 4),
                                    Text('Bills: $count', style: const TextStyle(color: Colors.black54)),
                                  ],
                                ),
                              );
                            },
                          ),
                        ),
                      Expanded(
                        child: ListView.separated(
                          itemCount: bills.length,
                          separatorBuilder: (_, __) => const Divider(height: 1),
                          itemBuilder: (ctx, i) {
                            final b = bills[i] as Map<String, dynamic>;
                    final items = (b['items'] as List<dynamic>? ?? const []).cast<Map<String, dynamic>>();
                    final partyId = b['partyId'];
                    final status = b['status']?.toString();
                    final age = _ageFromIso(b['createdAt']?.toString());
                    return ExpansionTile(
                      title: Text('Table #${b['tableNumber']} • ${b['paymentMethod']}'),
                      subtitle: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text('${b['mode']} • ${b['totalCents']} cents • ${status ?? ""}'),
                          const SizedBox(height: 6),
                          _slaChip(age, slaBillWarnMin, slaBillCritMin),
                        ],
                      ),
                      children: [
                        ...items.map((it) => ListTile(
                              title: Text('${it['name']} × ${it['qty']}'),
                              subtitle: Text('${it['lineTotalCents']} cents'),
                            )),
                        Padding(
                          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                          child: SizedBox(
                            width: double.infinity,
                            child: ElevatedButton(
                              onPressed: () => _confirmPaid(b['billRequestId']),
                              child: const Text('Confirm paid'),
                            ),
                          ),
                        ),
                        if (status == 'CREATED')
                          Padding(
                            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                            child: SizedBox(
                              width: double.infinity,
                              child: OutlinedButton(
                                onPressed: () => _cancelBill(b['billRequestId']),
                                child: const Text('Cancel bill'),
                              ),
                            ),
                          ),
                        if (partyId != null)
                          Padding(
                            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                            child: SizedBox(
                              width: double.infinity,
                              child: OutlinedButton(
                                onPressed: () async {
                                  final res = await http.post(
                                    Uri.parse('$apiBase/api/staff/parties/$partyId/close'),
                                    headers: {'Authorization': 'Basic $_auth'},
                                  );
                                  if (res.statusCode >= 300) {
                                    if (!context.mounted) return;
                                    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Close party failed (${res.statusCode})')));
                                    return;
                                  }
                                  if (!context.mounted) return;
                                  ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Party closed')));
                                },
                                child: const Text('Close party'),
                              ),
                            ),
                          ),
                      ],
                    );
                          },
                        ),
                      ),
                    ],
                  );
                }),
    );
  }
}

class KitchenTab extends StatefulWidget {
  final String username;
  final String password;

  const KitchenTab({super.key, required this.username, required this.password});

  @override
  State<KitchenTab> createState() => _KitchenTabState();
}

class _KitchenTabState extends State<KitchenTab> {
  bool _loading = true;
  String? _error;
  List<dynamic> _orders = const [];
  String _statusFilter = 'NEW,ACCEPTED,IN_PROGRESS';
  String _sortMode = 'time_desc';
  bool _showFocus = true;

  String get _auth => base64Encode(utf8.encode('${widget.username}:${widget.password}'));

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final res = await http.get(
        Uri.parse('$apiBase/api/staff/orders/kitchen?statusIn=$_statusFilter'),
        headers: {'Authorization': 'Basic $_auth'},
      );
      if (res.statusCode != 200) throw Exception('Load failed (${res.statusCode})');
      final body = jsonDecode(res.body);
      setState(() => _orders = body as List<dynamic>);
    } catch (e) {
      setState(() => _error = e.toString());
    } finally {
      setState(() => _loading = false);
    }
  }

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  Widget build(BuildContext context) {
    final orders = _orders.toList();
    orders.sort((a, b) {
      final sa = (a['status'] ?? '').toString();
      final sb = (b['status'] ?? '').toString();
      int pa = statusPriority(sa);
      int pb = statusPriority(sb);
      if (_sortMode == 'priority_time') {
        if (pa != pb) return pa.compareTo(pb);
        return (b['createdAt'] ?? '').toString().compareTo((a['createdAt'] ?? '').toString());
      }
      if (_sortMode == 'sla_desc') {
        final aa = (a['ageSeconds'] ?? 0) as int;
        final bb = (b['ageSeconds'] ?? 0) as int;
        return bb.compareTo(aa);
      }
      // time_desc default
      return (b['createdAt'] ?? '').toString().compareTo((a['createdAt'] ?? '').toString());
    });

    return Scaffold(
      appBar: AppBar(
        title: const Text('Kitchen Queue'),
        actions: [
          IconButton(
            onPressed: () => setState(() => _showFocus = !_showFocus),
            icon: Icon(_showFocus ? Icons.visibility : Icons.visibility_off),
          ),
          PopupMenuButton<String>(
            onSelected: (v) {
              setState(() => _statusFilter = v);
              _load();
            },
            itemBuilder: (ctx) => const [
              PopupMenuItem(value: 'NEW,ACCEPTED,IN_PROGRESS', child: Text('Active (New/Accepted/In Progress)')),
              PopupMenuItem(value: 'READY', child: Text('Ready')),
              PopupMenuItem(value: 'NEW', child: Text('New only')),
            ],
          ),
          PopupMenuButton<String>(
            onSelected: (v) => setState(() => _sortMode = v),
            itemBuilder: (ctx) => const [
              PopupMenuItem(value: 'time_desc', child: Text('Sort: newest first')),
              PopupMenuItem(value: 'priority_time', child: Text('Sort: priority then time')),
              PopupMenuItem(value: 'sla_desc', child: Text('Sort: SLA (oldest first)')),
            ],
          ),
          IconButton(onPressed: _load, icon: const Icon(Icons.refresh)),
        ],
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(child: Text(_error!))
              : Builder(builder: (context) {
                  int warn = 0;
                  int crit = 0;
                  final Map<int, Duration> maxAgeByTable = {};
                  final Map<int, int> countByTable = {};
                  for (final o in orders) {
                    final ageSec = (o['ageSeconds'] ?? 0) as int;
                    final tableNumber = (o['tableNumber'] ?? 0) as int;
                    final age = Duration(seconds: ageSec);
                    final prev = maxAgeByTable[tableNumber];
                    if (prev == null || age > prev) {
                      maxAgeByTable[tableNumber] = age;
                    }
                    countByTable[tableNumber] = (countByTable[tableNumber] ?? 0) + 1;
                    if (ageSec >= slaKitchenCritMin * 60) crit++;
                    else if (ageSec >= slaKitchenWarnMin * 60) warn++;
                  }
                  final focusTables = maxAgeByTable.entries.toList()
                    ..sort((a, b) => b.value.inSeconds.compareTo(a.value.inSeconds));
                  final topFocus = focusTables.take(3).toList();
                  return Column(
                    children: [
                      Padding(
                        padding: const EdgeInsets.fromLTRB(16, 12, 16, 4),
                        child: _slaSummaryRow(warn, crit),
                      ),
                      if (_showFocus && topFocus.isNotEmpty)
                        SizedBox(
                          height: 96,
                          child: ListView.separated(
                            padding: const EdgeInsets.fromLTRB(16, 8, 16, 8),
                            scrollDirection: Axis.horizontal,
                            itemCount: topFocus.length,
                            separatorBuilder: (_, __) => const SizedBox(width: 10),
                            itemBuilder: (ctx, i) {
                              final entry = topFocus[i];
                              final tableNo = entry.key;
                              final age = entry.value;
                              final count = countByTable[tableNo] ?? 0;
                              final color = _slaColor(age, slaKitchenWarnMin, slaKitchenCritMin);
                              final minutes = age.inMinutes < 1 ? '<1m' : '${age.inMinutes}m';
                              return Container(
                                width: 180,
                                padding: const EdgeInsets.all(12),
                                decoration: BoxDecoration(
                                  color: color.withOpacity(0.08),
                                  borderRadius: BorderRadius.circular(12),
                                  border: Border.all(color: color.withOpacity(0.4)),
                                ),
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  mainAxisAlignment: MainAxisAlignment.center,
                                  children: [
                                    Text('Table #$tableNo', style: const TextStyle(fontWeight: FontWeight.w700)),
                                    const SizedBox(height: 6),
                                    Text('Oldest: $minutes', style: TextStyle(color: color, fontWeight: FontWeight.w600)),
                                    const SizedBox(height: 4),
                                    Text('Kitchen: $count', style: const TextStyle(color: Colors.black54)),
                                  ],
                                ),
                              );
                            },
                          ),
                        ),
                      Expanded(
                        child: ListView.separated(
                          itemCount: orders.length,
                          separatorBuilder: (_, __) => const Divider(height: 1),
                          itemBuilder: (ctx, i) {
                            final o = orders[i] as Map<String, dynamic>;
                            final items = (o['items'] as List<dynamic>? ?? const []).cast<Map<String, dynamic>>();
                            final tableNumber = o['tableNumber'];
                            final ageSec = (o['ageSeconds'] ?? 0) as int;
                            final ageMin = (ageSec / 60).floor();
                            final age = Duration(seconds: ageSec);
                            return ListTile(
                              title: Text('Table #$tableNumber  •  Order #${o['id']}'),
                              subtitle: Text('${o['status']} • ${items.length} item(s) • ${ageMin}m'),
                              trailing: _slaChip(age, slaKitchenWarnMin, slaKitchenCritMin),
                              onTap: () {
                                Navigator.of(context).push(MaterialPageRoute(
                                  builder: (_) => OrderDetailsScreen(
                                    order: o,
                                    username: widget.username,
                                    password: widget.password,
                                    actions: const ['ACCEPTED', 'IN_PROGRESS', 'READY', 'SERVED'],
                                  ),
                                ));
                              },
                            );
                          },
                        ),
                      ),
                    ],
                  );
                }),
    );
  }

  int statusPriority(String status) {
    switch (status.toUpperCase()) {
      case 'NEW':
        return 0;
      case 'ACCEPTED':
        return 1;
      case 'COOKING':
      case 'IN_PROGRESS':
        return 2;
      case 'READY':
        return 3;
      default:
        return 9;
    }
  }
}

class FloorPlanTab extends StatefulWidget {
  final String username;
  final String password;

  const FloorPlanTab({super.key, required this.username, required this.password});

  @override
  State<FloorPlanTab> createState() => _FloorPlanTabState();
}

class _FloorPlanTabState extends State<FloorPlanTab> {
  bool _loading = true;
  String? _error;
  List<Map<String, dynamic>> _tables = const [];
  Set<int> _hotTableIds = {};
  String _bgUrl = '';
  List<Map<String, dynamic>> _zones = const [];
  List<Map<String, dynamic>> _halls = const [];
  int? _hallId;
  Timer? _pollTimer;
  Timer? _blinkTimer;
  bool _blinkOn = true;

  String get _auth => base64Encode(utf8.encode('${widget.username}:${widget.password}'));

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      List<Map<String, dynamic>> hallsBody = _halls;
      int? hallId = _hallId;
      final hallsRes = await http.get(
        Uri.parse('$apiBase/api/staff/halls'),
        headers: {'Authorization': 'Basic $_auth'},
      );
      if (hallsRes.statusCode == 200) {
        hallsBody = (jsonDecode(hallsRes.body) as List<dynamic>).cast<Map<String, dynamic>>();
        if (hallId == null && hallsBody.isNotEmpty) {
          hallId = (hallsBody.first['id'] as num).toInt();
        }
      }

      final tablesRes = await http.get(
        Uri.parse('$apiBase/api/staff/tables${hallId != null ? "?hallId=$hallId" : ""}'),
        headers: {'Authorization': 'Basic $_auth'},
      );
      if (tablesRes.statusCode != 200) throw Exception('Load tables failed (${tablesRes.statusCode})');
      final tablesBody = (jsonDecode(tablesRes.body) as List<dynamic>).cast<Map<String, dynamic>>();

      final ordersRes = await http.get(
        Uri.parse('$apiBase/api/staff/orders/active?statusIn=NEW'),
        headers: {'Authorization': 'Basic $_auth'},
      );
      if (ordersRes.statusCode != 200) throw Exception('Load orders failed (${ordersRes.statusCode})');
      final ordersBody = (jsonDecode(ordersRes.body) as List<dynamic>).cast<Map<String, dynamic>>();
      final hot = <int>{};
      for (final o in ordersBody) {
        final tableId = (o['tableId'] as num?)?.toInt();
        if (tableId != null) hot.add(tableId);
      }

      final layoutRes = await http.get(
        Uri.parse('$apiBase/api/staff/branch-layout${hallId != null ? "?hallId=$hallId" : ""}'),
        headers: {'Authorization': 'Basic $_auth'},
      );
      String bgUrl = '';
      List<Map<String, dynamic>> zones = const [];
      if (layoutRes.statusCode == 200) {
        final layout = jsonDecode(layoutRes.body);
        bgUrl = (layout['backgroundUrl'] ?? '').toString();
        final zonesJson = layout['zonesJson'];
        if (zonesJson != null && zonesJson.toString().isNotEmpty) {
          try {
            final parsed = jsonDecode(zonesJson.toString());
            if (parsed is List) {
              zones = parsed.cast<Map<String, dynamic>>();
            }
          } catch (_) {}
        }
      }

      setState(() {
        _tables = tablesBody;
        _hotTableIds = hot;
        _bgUrl = bgUrl;
        _zones = zones;
        _halls = hallsBody;
        _hallId = hallId;
      });
    } catch (e) {
      setState(() => _error = e.toString());
    } finally {
      setState(() => _loading = false);
    }
  }

  @override
  void initState() {
    super.initState();
    _load();
    _pollTimer = Timer.periodic(const Duration(seconds: 15), (_) => _load());
    _blinkTimer = Timer.periodic(const Duration(milliseconds: 600), (_) {
      if (mounted) setState(() => _blinkOn = !_blinkOn);
    });
  }

  @override
  void dispose() {
    _pollTimer?.cancel();
    _blinkTimer?.cancel();
    super.dispose();
  }

  Color _waiterColor(int? id) {
    const palette = [
      Color(0xFFFF6B6B),
      Color(0xFF4ECDC4),
      Color(0xFFFFD166),
      Color(0xFF6C5CE7),
      Color(0xFF00B894),
      Color(0xFFFD79A8),
      Color(0xFF0984E3),
    ];
    if (id == null) return Colors.grey.shade500;
    return palette[id % palette.length];
  }

  Map<String, num> _defaultLayout(int index) {
    final cols = 6;
    final col = index % cols;
    final row = index ~/ cols;
    return {
      'x': 5 + col * 15,
      'y': 6 + row * 16,
      'w': 10,
      'h': 10,
    };
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Hall'),
        actions: [
          if (_halls.isNotEmpty)
            DropdownButtonHideUnderline(
              child: DropdownButton<int>(
                value: _hallId,
                items: _halls
                    .map((h) => DropdownMenuItem<int>(
                          value: (h['id'] as num).toInt(),
                          child: Text(h['name']?.toString() ?? 'Hall'),
                        ))
                    .toList(),
                onChanged: (v) {
                  setState(() => _hallId = v);
                  _load();
                },
              ),
            ),
          IconButton(onPressed: _load, icon: const Icon(Icons.refresh)),
        ],
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(child: Text(_error!))
              : LayoutBuilder(builder: (context, constraints) {
                  final width = constraints.maxWidth;
                  final height = constraints.maxHeight;
                  return Stack(
                    children: [
                      Container(
                        decoration: const BoxDecoration(
                          gradient: LinearGradient(
                            colors: [Color(0xFFF7F8FA), Color(0xFFF1F3F7)],
                            begin: Alignment.topLeft,
                            end: Alignment.bottomRight,
                          ),
                        ),
                      ),
                      if (_bgUrl.isNotEmpty)
                        Positioned.fill(
                          child: Opacity(
                            opacity: 0.35,
                            child: Image.network(
                              _bgUrl,
                              fit: BoxFit.cover,
                            ),
                          ),
                        ),
                      Positioned.fill(
                        child: CustomPaint(
                          painter: _GridPainter(),
                        ),
                      ),
                      ..._zones.map((z) {
                        final x = (z['x'] as num?)?.toDouble() ?? 0;
                        final y = (z['y'] as num?)?.toDouble() ?? 0;
                        final w = (z['w'] as num?)?.toDouble() ?? 10;
                        final h = (z['h'] as num?)?.toDouble() ?? 10;
                        final name = (z['name'] ?? '').toString();
                        final colorStr = (z['color'] ?? '#6C5CE7').toString();
                        Color color;
                        try {
                          color = Color(int.parse(colorStr.replaceFirst('#', '0xff')));
                        } catch (_) {
                          color = const Color(0xFF6C5CE7);
                        }
                        return Positioned(
                          left: (x / 100) * width,
                          top: (y / 100) * height,
                          width: (w / 100) * width,
                          height: (h / 100) * height,
                          child: Container(
                            decoration: BoxDecoration(
                              borderRadius: BorderRadius.circular(16),
                              border: Border.all(color: color.withOpacity(0.7), style: BorderStyle.solid, width: 1),
                              color: color.withOpacity(0.12),
                            ),
                            padding: const EdgeInsets.all(6),
                            child: Align(
                              alignment: Alignment.topLeft,
                              child: Text(name, style: const TextStyle(fontSize: 11, color: Colors.black87)),
                            ),
                          ),
                        );
                      }),
                      ..._tables.asMap().entries.map((entry) {
                        final idx = entry.key;
                        final t = entry.value;
                        final def = _defaultLayout(idx);
                        final xPct = (t['layoutX'] as num?)?.toDouble() ?? def['x']!;
                        final yPct = (t['layoutY'] as num?)?.toDouble() ?? def['y']!;
                        final wPct = (t['layoutW'] as num?)?.toDouble() ?? def['w']!;
                        final hPct = (t['layoutH'] as num?)?.toDouble() ?? def['h']!;
                        final rotation = (t['layoutRotation'] as num?)?.toDouble() ?? 0;
                        final shape = (t['layoutShape'] as String?) ?? 'ROUND';
                        final tableId = (t['id'] as num?)?.toInt() ?? 0;
                        final waiterId = (t['assignedWaiterId'] as num?)?.toInt();
                        final isHot = _hotTableIds.contains(tableId);
                        final baseColor = _waiterColor(waiterId);
                        final glow = isHot && _blinkOn;

                        final left = (xPct / 100) * width;
                        final top = (yPct / 100) * height;
                        final w = (wPct / 100) * width;
                        final h = (hPct / 100) * height;

                        return Positioned(
                          left: left,
                          top: top,
                          width: w,
                          height: h,
                          child: Transform.rotate(
                            angle: rotation * 3.14159 / 180,
                            child: AnimatedContainer(
                              duration: const Duration(milliseconds: 300),
                              decoration: BoxDecoration(
                                color: Colors.white.withOpacity(0.95),
                                borderRadius: BorderRadius.circular(shape == 'ROUND' ? 999 : 14),
                                border: Border.all(color: glow ? Colors.redAccent : baseColor.withOpacity(0.7), width: glow ? 2.5 : 1.2),
                                boxShadow: [
                                  BoxShadow(
                                    color: glow ? Colors.redAccent.withOpacity(0.35) : Colors.black12,
                                    blurRadius: glow ? 18 : 10,
                                    spreadRadius: glow ? 1 : 0,
                                    offset: const Offset(0, 4),
                                  ),
                                ],
                              ),
                              child: Center(
                                child: Column(
                                  mainAxisSize: MainAxisSize.min,
                                  children: [
                                    Text('#${t['number']}', style: const TextStyle(fontWeight: FontWeight.w700)),
                                    const SizedBox(height: 4),
                                    Text(
                                      waiterId == null ? 'Unassigned' : 'Waiter #$waiterId',
                                      style: TextStyle(color: baseColor, fontSize: 11),
                                    ),
                                  ],
                                ),
                              ),
                            ),
                          ),
                        );
                      }),
                    ],
                  );
                }),
    );
  }
}

class _GridPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = Colors.black.withOpacity(0.04)
      ..strokeWidth = 1;
    const step = 24.0;
    for (double x = 0; x < size.width; x += step) {
      canvas.drawLine(Offset(x, 0), Offset(x, size.height), paint);
    }
    for (double y = 0; y < size.height; y += step) {
      canvas.drawLine(Offset(0, y), Offset(size.width, y), paint);
    }
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}

class HistoryTab extends StatefulWidget {
  final String username;
  final String password;

  const HistoryTab({super.key, required this.username, required this.password});

  @override
  State<HistoryTab> createState() => _HistoryTabState();
}

class _HistoryTabState extends State<HistoryTab> {
  bool _loading = true;
  String? _error;
  List<Map<String, dynamic>> _tables = const [];
  List<Map<String, dynamic>> _sessions = const [];
  List<Map<String, dynamic>> _orders = const [];
  int? _selectedTableId;
  int? _selectedGuestSessionId;

  String get _auth => base64Encode(utf8.encode('${widget.username}:${widget.password}'));

  Future<void> _loadTables() async {
    try {
      final res = await http.get(
        Uri.parse('$apiBase/api/staff/tables'),
        headers: {'Authorization': 'Basic $_auth'},
      );
      if (res.statusCode != 200) throw Exception('Load tables failed (${res.statusCode})');
      final body = (jsonDecode(res.body) as List<dynamic>).cast<Map<String, dynamic>>();
      setState(() {
        _tables = body;
        if (_tables.isNotEmpty) {
          _selectedTableId = (_tables.first['id'] as num).toInt();
        }
      });
    } catch (e) {
      setState(() => _error = e.toString());
    }
  }

  Future<void> _loadSessions() async {
    if (_selectedTableId == null) return;
    try {
      final res = await http.get(
        Uri.parse('$apiBase/api/staff/guest-sessions?tableId=$_selectedTableId'),
        headers: {'Authorization': 'Basic $_auth'},
      );
      if (res.statusCode != 200) throw Exception('Load sessions failed (${res.statusCode})');
      final body = (jsonDecode(res.body) as List<dynamic>).cast<Map<String, dynamic>>();
      setState(() {
        _sessions = body;
        _selectedGuestSessionId = null;
      });
    } catch (e) {
      setState(() => _error = e.toString());
    }
  }

  Future<void> _loadOrders() async {
    if (_selectedTableId == null) return;
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final params = <String>['tableId=$_selectedTableId'];
      if (_selectedGuestSessionId != null) {
        params.add('guestSessionId=$_selectedGuestSessionId');
      }
      final res = await http.get(
        Uri.parse('$apiBase/api/staff/orders/history?${params.join("&")}'),
        headers: {'Authorization': 'Basic $_auth'},
      );
      if (res.statusCode != 200) throw Exception('Load history failed (${res.statusCode})');
      final body = (jsonDecode(res.body) as List<dynamic>).cast<Map<String, dynamic>>();
      setState(() => _orders = body);
    } catch (e) {
      setState(() => _error = e.toString());
    } finally {
      setState(() => _loading = false);
    }
  }

  Future<void> _refreshAll() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    await _loadTables();
    await _loadSessions();
    await _loadOrders();
    setState(() => _loading = false);
  }

  @override
  void initState() {
    super.initState();
    _refreshAll();
  }

  @override
  Widget build(BuildContext context) {
    final groups = <int, List<Map<String, dynamic>>>{};
    for (final o in _orders) {
      final gs = (o['guestSessionId'] as num).toInt();
      groups.putIfAbsent(gs, () => []).add(o);
    }
    final groupKeys = groups.keys.toList()..sort((a, b) => b.compareTo(a));
    return Scaffold(
      appBar: AppBar(
        title: const Text('History'),
        actions: [
          IconButton(onPressed: _refreshAll, icon: const Icon(Icons.refresh)),
        ],
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(child: Text(_error!))
              : Column(
                  children: [
                    Padding(
                      padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
                      child: Row(
                        children: [
                          const Text('Table:'),
                          const SizedBox(width: 12),
                          Expanded(
                            child: DropdownButton<int?>(
                              isExpanded: true,
                              value: _selectedTableId,
                              items: _tables
                                  .map((t) => DropdownMenuItem<int?>(
                                        value: (t['id'] as num).toInt(),
                                        child: Text('Table #${t['number']}'),
                                      ))
                                  .toList(),
                              onChanged: (v) async {
                                setState(() => _selectedTableId = v);
                                await _loadSessions();
                                await _loadOrders();
                              },
                            ),
                          ),
                        ],
                      ),
                    ),
                    Padding(
                      padding: const EdgeInsets.fromLTRB(16, 8, 16, 12),
                      child: Row(
                        children: [
                          const Text('Guest:'),
                          const SizedBox(width: 12),
                          Expanded(
                            child: DropdownButton<int?>(
                              isExpanded: true,
                              value: _selectedGuestSessionId,
                              items: [
                                const DropdownMenuItem<int?>(value: null, child: Text('All guests')),
                                ..._sessions.map((s) {
                                  final id = (s['id'] as num).toInt();
                                  final lastOrderAt = s['lastOrderAt']?.toString();
                                  final label = lastOrderAt == null ? 'Guest #$id' : 'Guest #$id • last order';
                                  return DropdownMenuItem<int?>(value: id, child: Text(label));
                                }),
                              ],
                              onChanged: (v) async {
                                setState(() => _selectedGuestSessionId = v);
                                await _loadOrders();
                              },
                            ),
                          ),
                        ],
                      ),
                    ),
                    Expanded(
                      child: _orders.isEmpty
                          ? const Center(child: Text('No history for selection'))
                          : ListView.separated(
                              itemCount: groupKeys.length,
                              separatorBuilder: (_, __) => const Divider(height: 1),
                              itemBuilder: (ctx, i) {
                                final gs = groupKeys[i];
                                final list = groups[gs] ?? const [];
                                return ExpansionTile(
                                  title: Text('Guest #$gs • ${list.length} order(s)'),
                                  children: list.map((o) {
                                    final items = (o['items'] as List<dynamic>? ?? const []).cast<Map<String, dynamic>>();
                                    return ListTile(
                                      title: Text('Order #${o['id']} • ${o['status']}'),
                                      subtitle: Text('${o['createdAt']} • ${items.length} item(s)'),
                                      onTap: () {
                                        Navigator.of(context).push(MaterialPageRoute(
                                          builder: (_) => OrderDetailsScreen(
                                            order: o,
                                            username: widget.username,
                                            password: widget.password,
                                            actions: const ['ACCEPTED', 'IN_PROGRESS', 'READY', 'SERVED', 'CLOSED', 'CANCELLED'],
                                          ),
                                        ));
                                      },
                                    );
                                  }).toList(),
                                );
                              },
                            ),
                    ),
                  ],
                ),
    );
  }
}

class OrderDetailsScreen extends StatelessWidget {
  final Map<String, dynamic> order;
  final String username;
  final String password;
  final List<String> actions;

  const OrderDetailsScreen({
    super.key,
    required this.order,
    required this.username,
    required this.password,
    required this.actions,
  });

  String get _auth => base64Encode(utf8.encode('$username:$password'));

  Future<void> _setStatus(BuildContext context, String status) async {
    final id = order['id'];
    final res = await http.post(
      Uri.parse('$apiBase/api/staff/orders/$id/status'),
      headers: {'Authorization': 'Basic $_auth', 'Content-Type': 'application/json'},
      body: jsonEncode({'status': status}),
    );
    if (res.statusCode >= 300) {
      if (!context.mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Status update failed (${res.statusCode})')));
      return;
    }
    if (!context.mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Status -> $status')));
  }

  @override
  Widget build(BuildContext context) {
    final items = (order['items'] as List<dynamic>? ?? const []).cast<Map<String, dynamic>>();
    return Scaffold(
      appBar: AppBar(title: Text('Order #${order['id']}')),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Table #${order['tableNumber']}', style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 8),
            Text('Status: ${order['status']}'),
            const SizedBox(height: 16),
            Expanded(
              child: ListView.builder(
                itemCount: items.length,
                itemBuilder: (_, i) {
                  final it = items[i];
                  return ListTile(
                    title: Text('${it['name']}  × ${it['qty']}'),
                    subtitle: it['comment'] != null ? Text(it['comment']) : null,
                  );
                },
              ),
            ),
            Row(
              children: actions
                  .map((s) => Expanded(
                        child: Padding(
                          padding: const EdgeInsets.only(right: 8),
                          child: OutlinedButton(
                            onPressed: () => _setStatus(context, s),
                            child: Text(s),
                          ),
                        ),
                      ))
                  .toList(),
            ),
          ],
        ),
      ),
    );
  }
}
