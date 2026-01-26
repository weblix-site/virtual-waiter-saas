import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;

void main() {
  runApp(const StaffApp());
}

const String apiBase = String.fromEnvironment('API_BASE', defaultValue: 'http://localhost:8080');

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
        MaterialPageRoute(builder: (_) => OrdersScreen(username: _user.text, password: _pass.text)),
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

class OrdersScreen extends StatefulWidget {
  final String username;
  final String password;

  const OrdersScreen({super.key, required this.username, required this.password});

  @override
  State<OrdersScreen> createState() => _OrdersScreenState();
}

class _OrdersScreenState extends State<OrdersScreen> {
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
                    return ListTile(
                      title: Text('Table #$tableNumber  •  Order #${o['id']}'),
                      subtitle: Text('${o['status']} • ${items.length} item(s)'),
                      onTap: () {
                        Navigator.of(context).push(MaterialPageRoute(
                          builder: (_) => OrderDetailsScreen(order: o, username: widget.username, password: widget.password),
                        ));
                      },
                    );
                  },
                ),
    );
  }
}

class OrderDetailsScreen extends StatelessWidget {
  final Map<String, dynamic> order;
  final String username;
  final String password;

  const OrderDetailsScreen({super.key, required this.order, required this.username, required this.password});

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
              children: [
                Expanded(child: OutlinedButton(onPressed: () => _setStatus(context, 'ACCEPTED'), child: const Text('ACCEPT'))),
                const SizedBox(width: 8),
                Expanded(child: OutlinedButton(onPressed: () => _setStatus(context, 'READY'), child: const Text('READY'))),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
