import 'dart:convert';
import 'dart:async';
import 'dart:math';

import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:audioplayers/audioplayers.dart';

void main() {
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

  String _genToken() {
    const chars = 'abcdefghijklmnopqrstuvwxyz0123456789';
    final rnd = Random();
    return List.generate(32, (_) => chars[rnd.nextInt(chars.length)]).join();
  }

  @override
  void initState() {
    super.initState();
    _timer = Timer.periodic(const Duration(seconds: 15), (_) => _poll());
    _poll();
    _registerDevice();
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
          IconButton(onPressed: _load, icon: const Icon(Icons.refresh)),
        ],
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(child: Text(_error!))
              : ListView.separated(
                  itemCount: _orders.length,
                  separatorBuilder: (_, __) => const Divider(height: 1),
                  itemBuilder: (ctx, i) {
                    final o = _orders[i] as Map<String, dynamic>;
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
                            actions: const ['ACCEPTED', 'READY'],
                          ),
                        ));
                      },
                    );
                  },
                ),
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
          IconButton(onPressed: _load, icon: const Icon(Icons.refresh)),
        ],
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(child: Text(_error!))
              : ListView.separated(
                  itemCount: _calls.length,
                  separatorBuilder: (_, __) => const Divider(height: 1),
                itemBuilder: (ctx, i) {
                  final c = _calls[i] as Map<String, dynamic>;
                  final age = _ageFromIso(c['createdAt']?.toString());
                  return ListTile(
                    title: Text('Table #${c['tableNumber']}'),
                    subtitle: Text('${c['status']} • ${c['createdAt']}'),
                    leading: const Icon(Icons.notifications_active),
                    trailing: _slaChip(age, slaCallWarnMin, slaCallCritMin),
                  );
                },
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
          IconButton(onPressed: _load, icon: const Icon(Icons.refresh)),
        ],
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(child: Text(_error!))
              : ListView.separated(
                  itemCount: _bills.length,
                  separatorBuilder: (_, __) => const Divider(height: 1),
                  itemBuilder: (ctx, i) {
                    final b = _bills[i] as Map<String, dynamic>;
                    final items = (b['items'] as List<dynamic>? ?? const []).cast<Map<String, dynamic>>();
                    final partyId = b['partyId'];
                    final age = _ageFromIso(b['createdAt']?.toString());
                    return ExpansionTile(
                      title: Text('Table #${b['tableNumber']} • ${b['paymentMethod']}'),
                      subtitle: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text('${b['mode']} • ${b['totalCents']} cents'),
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
  String _statusFilter = 'NEW,ACCEPTED,COOKING';
  String _sortMode = 'time_desc';

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
      // time_desc default
      return (b['createdAt'] ?? '').toString().compareTo((a['createdAt'] ?? '').toString());
    });

    return Scaffold(
      appBar: AppBar(
        title: const Text('Kitchen Queue'),
        actions: [
          PopupMenuButton<String>(
            onSelected: (v) {
              setState(() => _statusFilter = v);
              _load();
            },
            itemBuilder: (ctx) => const [
              PopupMenuItem(value: 'NEW,ACCEPTED,COOKING', child: Text('Active (New/Accepted/Cooking)')),
              PopupMenuItem(value: 'READY', child: Text('Ready')),
              PopupMenuItem(value: 'NEW', child: Text('New only')),
            ],
          ),
          PopupMenuButton<String>(
            onSelected: (v) => setState(() => _sortMode = v),
            itemBuilder: (ctx) => const [
              PopupMenuItem(value: 'time_desc', child: Text('Sort: newest first')),
              PopupMenuItem(value: 'priority_time', child: Text('Sort: priority then time')),
            ],
          ),
          IconButton(onPressed: _load, icon: const Icon(Icons.refresh)),
        ],
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(child: Text(_error!))
              : ListView.separated(
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
                            actions: const ['ACCEPTED', 'COOKING', 'READY'],
                          ),
                        ));
                      },
                    );
                  },
                ),
    );
  }

  int statusPriority(String status) {
    switch (status.toUpperCase()) {
      case 'NEW':
        return 0;
      case 'ACCEPTED':
        return 1;
      case 'COOKING':
        return 2;
      case 'READY':
        return 3;
      default:
        return 9;
    }
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
