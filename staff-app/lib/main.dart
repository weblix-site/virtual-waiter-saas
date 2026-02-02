import 'dart:convert';
import 'dart:async';
import 'dart:math';

import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:audioplayers/audioplayers.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:shared_preferences/shared_preferences.dart';

@pragma('vm:entry-point')
Future<void> _firebaseMessagingBackgroundHandler(RemoteMessage message) async {
  await Firebase.initializeApp();
}

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await Firebase.initializeApp();
  FirebaseMessaging.onBackgroundMessage(_firebaseMessagingBackgroundHandler);
  await SlaConfig.load();
  runApp(const StaffApp());
}

const String apiBase = String.fromEnvironment('API_BASE', defaultValue: 'http://localhost:8080');

class SlaConfig {
  static int orderWarn = 5;
  static int orderCrit = 10;
  static int callWarn = 2;
  static int callCrit = 5;
  static int billWarn = 5;
  static int billCrit = 10;
  static int kitchenWarn = 7;
  static int kitchenCrit = 15;

  static Future<void> load() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      orderWarn = prefs.getInt('sla_order_warn') ?? orderWarn;
      orderCrit = prefs.getInt('sla_order_crit') ?? orderCrit;
      callWarn = prefs.getInt('sla_call_warn') ?? callWarn;
      callCrit = prefs.getInt('sla_call_crit') ?? callCrit;
      billWarn = prefs.getInt('sla_bill_warn') ?? billWarn;
      billCrit = prefs.getInt('sla_bill_crit') ?? billCrit;
      kitchenWarn = prefs.getInt('sla_kitchen_warn') ?? kitchenWarn;
      kitchenCrit = prefs.getInt('sla_kitchen_crit') ?? kitchenCrit;
    } catch (_) {}
  }

  static Future<void> save({
    required int orderWarn,
    required int orderCrit,
    required int callWarn,
    required int callCrit,
    required int billWarn,
    required int billCrit,
    required int kitchenWarn,
    required int kitchenCrit,
  }) async {
    SlaConfig.orderWarn = orderWarn;
    SlaConfig.orderCrit = orderCrit;
    SlaConfig.callWarn = callWarn;
    SlaConfig.callCrit = callCrit;
    SlaConfig.billWarn = billWarn;
    SlaConfig.billCrit = billCrit;
    SlaConfig.kitchenWarn = kitchenWarn;
    SlaConfig.kitchenCrit = kitchenCrit;
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setInt('sla_order_warn', orderWarn);
      await prefs.setInt('sla_order_crit', orderCrit);
      await prefs.setInt('sla_call_warn', callWarn);
      await prefs.setInt('sla_call_crit', callCrit);
      await prefs.setInt('sla_bill_warn', billWarn);
      await prefs.setInt('sla_bill_crit', billCrit);
      await prefs.setInt('sla_kitchen_warn', kitchenWarn);
      await prefs.setInt('sla_kitchen_crit', kitchenCrit);
    } catch (_) {}
  }

  static Future<void> resetDefaults() async {
    await save(
      orderWarn: 5,
      orderCrit: 10,
      callWarn: 2,
      callCrit: 5,
      billWarn: 5,
      billCrit: 10,
      kitchenWarn: 7,
      kitchenCrit: 15,
    );
  }
}

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

Widget _slaSummaryRow(BuildContext context, int warn, int crit) {
  return Row(
    children: [
      Container(
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
        decoration: BoxDecoration(
          color: Colors.orange.withOpacity(0.1),
          borderRadius: BorderRadius.circular(999),
          border: Border.all(color: Colors.orange, width: 1),
        ),
        child: Text('${_tr(context, 'warn')}: $warn', style: const TextStyle(color: Colors.orange, fontWeight: FontWeight.w600)),
      ),
      const SizedBox(width: 8),
      Container(
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
        decoration: BoxDecoration(
          color: Colors.red.withOpacity(0.1),
          borderRadius: BorderRadius.circular(999),
          border: Border.all(color: Colors.red, width: 1),
        ),
        child: Text('${_tr(context, 'crit')}: $crit', style: const TextStyle(color: Colors.red, fontWeight: FontWeight.w600)),
      ),
    ],
  );
}

String _tr(BuildContext context, String key) {
  final lang = Localizations.localeOf(context).languageCode.toLowerCase();
  const dict = {
    'staffLogin': {'ru': 'Вход персонала', 'ro': 'Autentificare personal', 'en': 'Staff Login'},
    'username': {'ru': 'Логин', 'ro': 'Utilizator', 'en': 'Username'},
    'role': {'ru': 'Роль', 'ro': 'Rol', 'en': 'Role'},
    'password': {'ru': 'Пароль', 'ro': 'Parolă', 'en': 'Password'},
    'login': {'ru': 'Войти', 'ro': 'Autentificare', 'en': 'Login'},
    'loading': {'ru': 'Загрузка...', 'ro': 'Se încarcă...', 'en': 'Loading...'},
    'demoCreds': {'ru': 'Демо: waiter1 / demo123', 'ro': 'Demo: waiter1 / demo123', 'en': 'Demo: waiter1 / demo123'},
    'loginFailed': {'ru': 'Ошибка входа', 'ro': 'Autentificare eșuată', 'en': 'Login failed'},
    'orders': {'ru': 'Заказы', 'ro': 'Comenzi', 'en': 'Orders'},
    'calls': {'ru': 'Вызовы', 'ro': 'Apeluri', 'en': 'Calls'},
    'bills': {'ru': 'Счета', 'ro': 'Note', 'en': 'Bills'},
    'kitchen': {'ru': 'Кухня', 'ro': 'Bucătărie', 'en': 'Kitchen'},
    'hall': {'ru': 'Зал', 'ro': 'Sală', 'en': 'Hall'},
    'lastHall': {'ru': 'Последний зал', 'ro': 'Ultima sală', 'en': 'Last hall'},
    'lastHallLabel': {'ru': 'Последний зал: ', 'ro': 'Ultima sală: ', 'en': 'Last hall: '},
    'resetHall': {'ru': 'Сбросить зал', 'ro': 'Resetează sala', 'en': 'Reset hall'},
    'history': {'ru': 'История', 'ro': 'Istoric', 'en': 'History'},
    'events': {'ru': 'События', 'ro': 'Evenimente', 'en': 'Events'},
    'profile': {'ru': 'Профиль', 'ro': 'Profil', 'en': 'Profile'},
    'myProfile': {'ru': 'Мой профиль', 'ro': 'Profilul meu', 'en': 'My profile'},
    'firstName': {'ru': 'Имя', 'ro': 'Prenume', 'en': 'First name'},
    'lastName': {'ru': 'Фамилия', 'ro': 'Nume', 'en': 'Last name'},
    'age': {'ru': 'Возраст', 'ro': 'Vârstă', 'en': 'Age'},
    'gender': {'ru': 'Пол', 'ro': 'Gen', 'en': 'Gender'},
    'genderMale': {'ru': 'Мужской', 'ro': 'Masculin', 'en': 'Male'},
    'genderFemale': {'ru': 'Женский', 'ro': 'Feminin', 'en': 'Female'},
    'genderOther': {'ru': 'Другое', 'ro': 'Altul', 'en': 'Other'},
    'photoUrl': {'ru': 'Фото', 'ro': 'Foto', 'en': 'Photo'},
    'notSet': {'ru': 'не задано', 'ro': 'nesetat', 'en': 'not set'},
    'refresh': {'ru': 'Обновить', 'ro': 'Reîmprospătează', 'en': 'Refresh'},
    'sortNewest': {'ru': 'Сорт: новые сверху', 'ro': 'Sortare: cele mai noi', 'en': 'Sort: newest first'},
    'sortSla': {'ru': 'Сорт: SLA (старые сверху)', 'ro': 'Sortare: SLA (cele mai vechi)', 'en': 'Sort: SLA (oldest first)'},
    'sortPriority': {'ru': 'Сорт: приоритет → время', 'ro': 'Sortare: prioritate → timp', 'en': 'Sort: priority then time'},
    'kitchenActive': {'ru': 'Активные (NEW/ACCEPTED/IN_PROGRESS)', 'ro': 'Active (NEW/ACCEPTED/IN_PROGRESS)', 'en': 'Active (NEW/ACCEPTED/IN_PROGRESS)'},
    'kitchenReady': {'ru': 'Готово', 'ro': 'Gata', 'en': 'Ready'},
    'kitchenNewOnly': {'ru': 'Только NEW', 'ro': 'Doar NEW', 'en': 'New only'},
    'confirmPaid': {'ru': 'Подтвердить оплату', 'ro': 'Confirmă plata', 'en': 'Confirm paid'},
    'cancelBill': {'ru': 'Отменить счёт', 'ro': 'Anulează nota', 'en': 'Cancel bill'},
    'paymentConfirmed': {'ru': 'Оплата подтверждена', 'ro': 'Plată confirmată', 'en': 'Payment confirmed'},
    'billCancelled': {'ru': 'Счёт отменён', 'ro': 'Nota anulată', 'en': 'Bill cancelled'},
    'closeParty': {'ru': 'Закрыть Party', 'ro': 'Închide Party', 'en': 'Close party'},
    'partyClosed': {'ru': 'Party закрыт', 'ro': 'Party închis', 'en': 'Party closed'},
    'markServed': {'ru': 'Выдано', 'ro': 'Servit', 'en': 'Served'},
    'closeOrder': {'ru': 'Закрыть', 'ro': 'Închide', 'en': 'Close'},
    'confirm': {'ru': 'Подтверждение', 'ro': 'Confirmare', 'en': 'Confirm'},
    'confirmCloseOrder': {'ru': 'Закрыть заказ?', 'ro': 'Închide comanda?', 'en': 'Close order?'},
    'confirmCancelBill': {'ru': 'Отменить счёт?', 'ro': 'Anulează nota?', 'en': 'Cancel bill?'},
    'confirmCloseParty': {'ru': 'Закрыть Party?', 'ro': 'Închide Party?', 'en': 'Close party?'},
    'confirmPaidQuestion': {'ru': 'Подтвердить оплату?', 'ro': 'Confirmă plata?', 'en': 'Confirm paid?'},
    'slaSettings': {'ru': 'Настройки SLA', 'ro': 'Setări SLA', 'en': 'SLA settings'},
    'save': {'ru': 'Сохранить', 'ro': 'Salvează', 'en': 'Save'},
    'resetDefaults': {'ru': 'Сбросить', 'ro': 'Resetare', 'en': 'Reset'},
    'heatmap': {'ru': 'Теплокарта', 'ro': 'Heatmap', 'en': 'Heatmap'},
    'low': {'ru': 'Низкая', 'ro': 'Scăzută', 'en': 'Low'},
    'medium': {'ru': 'Средняя', 'ro': 'Mediu', 'en': 'Medium'},
    'high': {'ru': 'Высокая', 'ro': 'Ridicată', 'en': 'High'},
    'waiterCalls': {'ru': 'Вызовы официанта', 'ro': 'Apeluri chelner', 'en': 'Waiter Calls'},
    'billRequests': {'ru': 'Запросы счёта', 'ro': 'Cereri de plată', 'en': 'Bill Requests'},
    'kitchenQueue': {'ru': 'Очередь кухни', 'ro': 'Coadă bucătărie', 'en': 'Kitchen Queue'},
    'table': {'ru': 'Стол', 'ro': 'Masă', 'en': 'Table'},
    'oldest': {'ru': 'Самый старый', 'ro': 'Cel mai vechi', 'en': 'Oldest'},
    'ordersCount': {'ru': 'Заказы', 'ro': 'Comenzi', 'en': 'Orders'},
    'callsCount': {'ru': 'Вызовы', 'ro': 'Apeluri', 'en': 'Calls'},
    'billsCount': {'ru': 'Счета', 'ro': 'Note', 'en': 'Bills'},
    'unassigned': {'ru': 'Не назначен', 'ro': 'Neatribuit', 'en': 'Unassigned'},
    'waiter': {'ru': 'Официант', 'ro': 'Chelner', 'en': 'Waiter'},
    'plans': {'ru': 'Планы', 'ro': 'Planuri', 'en': 'Plans'},
    'useActive': {'ru': 'Использовать активный', 'ro': 'Folosește activ', 'en': 'Use active'},
    'legend': {'ru': 'Легенда', 'ro': 'Legendă', 'en': 'Legend'},
    'active': {'ru': 'Активный', 'ro': 'Activ', 'en': 'Active'},
    'status': {'ru': 'Статус', 'ro': 'Status', 'en': 'Status'},
    'statusNEW': {'ru': 'Новый', 'ro': 'Nou', 'en': 'New'},
    'statusACCEPTED': {'ru': 'Принят', 'ro': 'Acceptat', 'en': 'Accepted'},
    'statusIN_PROGRESS': {'ru': 'Готовится', 'ro': 'În lucru', 'en': 'In progress'},
    'statusREADY': {'ru': 'Готово', 'ro': 'Gata', 'en': 'Ready'},
    'statusSERVED': {'ru': 'Выдано', 'ro': 'Servit', 'en': 'Served'},
    'statusCLOSED': {'ru': 'Закрыт', 'ro': 'Închis', 'en': 'Closed'},
    'statusCANCELLED': {'ru': 'Отменён', 'ro': 'Anulat', 'en': 'Cancelled'},
    'historyTitle': {'ru': 'История', 'ro': 'Istoric', 'en': 'History'},
    'tableLabel': {'ru': 'Стол', 'ro': 'Masă', 'en': 'Table'},
    'guestLabel': {'ru': 'Гость', 'ro': 'Oaspete', 'en': 'Guest'},
    'allGuests': {'ru': 'Все гости', 'ro': 'Toți oaspeții', 'en': 'All guests'},
    'lastOrder': {'ru': 'последний заказ', 'ro': 'ultima comandă', 'en': 'last order'},
    'noHistory': {'ru': 'Нет истории по выбранным фильтрам', 'ro': 'Nu există istoric pentru selecție', 'en': 'No history for selection'},
    'order': {'ru': 'Заказ', 'ro': 'Comandă', 'en': 'Order'},
    'items': {'ru': 'позиции', 'ro': 'poziții', 'en': 'items'},
    'warn': {'ru': 'Предупр.', 'ro': 'Avert.', 'en': 'Warn'},
    'crit': {'ru': 'Крит.', 'ro': 'Crit.', 'en': 'Crit'},
    'cents': {'ru': 'центов', 'ro': 'cenți', 'en': 'cents'},
    'kitchenCount': {'ru': 'Кухня', 'ro': 'Bucătărie', 'en': 'Kitchen'},
    'notification': {'ru': 'Уведомление', 'ro': 'Notificare', 'en': 'Notification'},
    'showStatusBadges': {'ru': 'Показать статусы', 'ro': 'Afișează statusuri', 'en': 'Show status badges'},
    'hideStatusBadges': {'ru': 'Скрыть статусы', 'ro': 'Ascunde statusuri', 'en': 'Hide status badges'},
    'hallFallback': {'ru': 'Зал', 'ro': 'Sală', 'en': 'Hall'},
    'back': {'ru': 'Назад', 'ro': 'Înapoi', 'en': 'Back'},
    'dateFrom': {'ru': 'С', 'ro': 'De la', 'en': 'From'},
    'dateTo': {'ru': 'По', 'ro': 'Până la', 'en': 'To'},
    'selectDate': {'ru': 'Выбрать', 'ro': 'Selectează', 'en': 'Select'},
    'clear': {'ru': 'Очистить', 'ro': 'Curăță', 'en': 'Clear'},
    'today': {'ru': 'Сегодня', 'ro': 'Astăzi', 'en': 'Today'},
    'last7days': {'ru': '7 дней', 'ro': '7 zile', 'en': '7 days'},
    'last30days': {'ru': '30 дней', 'ro': '30 zile', 'en': '30 days'},
    'closeCallTitle': {'ru': 'Закрыть вызов?', 'ro': 'Închide apelul?', 'en': 'Close call?'},
    'closeCallBody': {'ru': 'Отметить вызов официанта как закрытый?', 'ro': 'Marcați apelul chelnerului ca închis?', 'en': 'Mark this waiter call as closed?'},
    'cancel': {'ru': 'Отмена', 'ro': 'Anulează', 'en': 'Cancel'},
    'close': {'ru': 'Закрыть', 'ro': 'Închide', 'en': 'Close'},
    'ack': {'ru': 'Принять', 'ro': 'Confirmă', 'en': 'Ack'},
    'statusUpdated': {'ru': 'Статус обновлён', 'ro': 'Status actualizat', 'en': 'Status updated'},
    'statusUpdateFailed': {'ru': 'Не удалось обновить статус', 'ro': 'Actualizare status eșuată', 'en': 'Status update failed'},
    'confirmFailed': {'ru': 'Не удалось подтвердить', 'ro': 'Confirmare eșuată', 'en': 'Confirm failed'},
    'cancelFailed': {'ru': 'Не удалось отменить', 'ro': 'Anulare eșuată', 'en': 'Cancel failed'},
    'closePartyFailed': {'ru': 'Не удалось закрыть Party', 'ro': 'Nu s-a putut închide Party', 'en': 'Close party failed'},
    'loadFailed': {'ru': 'Ошибка загрузки', 'ro': 'Încărcare eșuată', 'en': 'Load failed'},
    'loadTablesFailed': {'ru': 'Не удалось загрузить столы', 'ro': 'Nu s-au putut încărca mesele', 'en': 'Load tables failed'},
    'loadOrdersFailed': {'ru': 'Не удалось загрузить заказы', 'ro': 'Nu s-au putut încărca comenzile', 'en': 'Load orders failed'},
    'loadSessionsFailed': {'ru': 'Не удалось загрузить сессии', 'ro': 'Nu s-au putut încărca sesiunile', 'en': 'Load sessions failed'},
    'loadHistoryFailed': {'ru': 'Не удалось загрузить историю', 'ro': 'Nu s-a putut încărca istoricul', 'en': 'Load history failed'},
    'noEvents': {'ru': 'Событий пока нет', 'ro': 'Nu există evenimente', 'en': 'No events yet'},
    'newEvents': {'ru': 'Новые события', 'ro': 'Evenimente noi', 'en': 'New events'},
    'push': {'ru': 'Push', 'ro': 'Push', 'en': 'Push'},
  };
  final entry = dict[key];
  if (entry == null) return key;
  return entry[lang] ?? entry['ru']!;
}

Future<bool> _confirmAction(
  BuildContext context, {
  required String titleKey,
  required String contentKey,
  required String confirmKey,
}) async {
  final res = await showDialog<bool>(
    context: context,
    builder: (ctx) => AlertDialog(
      title: Text(_tr(ctx, titleKey)),
      content: Text(_tr(ctx, contentKey)),
      actions: [
        TextButton(onPressed: () => Navigator.of(ctx).pop(false), child: Text(_tr(ctx, 'cancel'))),
        ElevatedButton(onPressed: () => Navigator.of(ctx).pop(true), child: Text(_tr(ctx, confirmKey))),
      ],
    ),
  );
  return res == true;
}

String _statusLabel(BuildContext context, String status) {
  final s = status.toUpperCase();
  switch (s) {
    case 'NEW':
      return _tr(context, 'statusNEW');
    case 'ACCEPTED':
      return _tr(context, 'statusACCEPTED');
    case 'IN_PROGRESS':
      return _tr(context, 'statusIN_PROGRESS');
    case 'READY':
      return _tr(context, 'statusREADY');
    case 'SERVED':
      return _tr(context, 'statusSERVED');
    case 'CLOSED':
      return _tr(context, 'statusCLOSED');
    case 'CANCELLED':
      return _tr(context, 'statusCANCELLED');
    default:
      return status;
  }
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
        throw Exception('${_tr(context, 'loginFailed')} (${res.statusCode})');
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
      appBar: AppBar(title: Text(_tr(context, 'staffLogin'))),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            TextField(controller: _user, decoration: InputDecoration(labelText: _tr(context, 'username'))),
            const SizedBox(height: 8),
            TextField(controller: _pass, decoration: InputDecoration(labelText: _tr(context, 'password')), obscureText: true),
            const SizedBox(height: 16),
            if (_error != null) Text(_error!, style: const TextStyle(color: Colors.red)),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                onPressed: _loading ? null : _login,
                child: Text(_loading ? _tr(context, 'loading') : _tr(context, 'login')),
              ),
            ),
            const SizedBox(height: 12),
            Text(_tr(context, 'demoCreds')),
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
  List<Map<String, dynamic>> _halls = const [];
  int? _hallId;
  static const String _lastHallPrefKey = 'staff_last_hall_id';
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
  int? _lastHallPrefId;

  String get _auth => base64Encode(utf8.encode('${widget.username}:${widget.password}'));

  Future<void> _showSlaDialog() async {
    int clampInt(String v, int fallback) {
      final n = int.tryParse(v.trim());
      if (n == null || n <= 0) return fallback;
      return n;
    }

    final orderWarnCtrl = TextEditingController(text: SlaConfig.orderWarn.toString());
    final orderCritCtrl = TextEditingController(text: SlaConfig.orderCrit.toString());
    final callWarnCtrl = TextEditingController(text: SlaConfig.callWarn.toString());
    final callCritCtrl = TextEditingController(text: SlaConfig.callCrit.toString());
    final billWarnCtrl = TextEditingController(text: SlaConfig.billWarn.toString());
    final billCritCtrl = TextEditingController(text: SlaConfig.billCrit.toString());
    final kitchenWarnCtrl = TextEditingController(text: SlaConfig.kitchenWarn.toString());
    final kitchenCritCtrl = TextEditingController(text: SlaConfig.kitchenCrit.toString());

    await showDialog<void>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(_tr(ctx, 'slaSettings')),
        content: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              _homeSlaRow(ctx, _tr(ctx, 'orders'), orderWarnCtrl, orderCritCtrl),
              _homeSlaRow(ctx, _tr(ctx, 'calls'), callWarnCtrl, callCritCtrl),
              _homeSlaRow(ctx, _tr(ctx, 'bills'), billWarnCtrl, billCritCtrl),
              _homeSlaRow(ctx, _tr(ctx, 'kitchen'), kitchenWarnCtrl, kitchenCritCtrl),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () async {
              await SlaConfig.resetDefaults();
              if (!ctx.mounted) return;
              Navigator.of(ctx).pop();
              if (mounted) setState(() {});
            },
            child: Text(_tr(ctx, 'resetDefaults')),
          ),
          TextButton(onPressed: () => Navigator.of(ctx).pop(), child: Text(_tr(ctx, 'cancel'))),
          ElevatedButton(
            onPressed: () async {
              int ow = clampInt(orderWarnCtrl.text, 5);
              int oc = clampInt(orderCritCtrl.text, 10);
              int cw = clampInt(callWarnCtrl.text, 2);
              int cc = clampInt(callCritCtrl.text, 5);
              int bw = clampInt(billWarnCtrl.text, 5);
              int bc = clampInt(billCritCtrl.text, 10);
              int kw = clampInt(kitchenWarnCtrl.text, 7);
              int kc = clampInt(kitchenCritCtrl.text, 15);
              if (oc <= ow) oc = ow + 1;
              if (cc <= cw) cc = cw + 1;
              if (bc <= bw) bc = bw + 1;
              if (kc <= kw) kc = kw + 1;
              await SlaConfig.save(
                orderWarn: ow,
                orderCrit: oc,
                callWarn: cw,
                callCrit: cc,
                billWarn: bw,
                billCrit: bc,
                kitchenWarn: kw,
                kitchenCrit: kc,
              );
              if (!ctx.mounted) return;
              Navigator.of(ctx).pop();
              if (mounted) setState(() {});
            },
            child: Text(_tr(ctx, 'save')),
          ),
        ],
      ),
    );
  }

  Widget _homeSlaRow(BuildContext ctx, String label, TextEditingController warnCtrl, TextEditingController critCtrl) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: Row(
        children: [
          Expanded(child: Text(label)),
          SizedBox(
            width: 70,
            child: TextField(
              controller: warnCtrl,
              keyboardType: TextInputType.number,
              decoration: InputDecoration(labelText: _tr(ctx, 'warn'), isDense: true),
            ),
          ),
          const SizedBox(width: 8),
          SizedBox(
            width: 70,
            child: TextField(
              controller: critCtrl,
              keyboardType: TextInputType.number,
              decoration: InputDecoration(labelText: _tr(ctx, 'crit'), isDense: true),
            ),
          ),
        ],
      ),
    );
  }

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
              if (orders > 0) parts.add('${_tr(context, 'ordersCount')}: $orders');
              if (calls > 0) parts.add('${_tr(context, 'callsCount')}: $calls');
              if (bills > 0) parts.add('${_tr(context, 'billsCount')}: $bills');
              if (parts.isNotEmpty) {
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(content: Text('${_tr(context, 'newEvents')} • ${parts.join("  ")}')),
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

  Future<void> _loadHalls() async {
    try {
      final res = await http.get(
        Uri.parse('$apiBase/api/staff/halls'),
        headers: {'Authorization': 'Basic $_auth'},
      );
      if (res.statusCode != 200) return;
      final body = (jsonDecode(res.body) as List<dynamic>).cast<Map<String, dynamic>>();
      int? nextHallId = _hallId;
      if (body.isNotEmpty) {
        if (nextHallId == null) {
          nextHallId = (body.first['id'] as num).toInt();
        } else {
          final exists = body.any((h) => (h['id'] as num?)?.toInt() == nextHallId);
          if (!exists) nextHallId = (body.first['id'] as num).toInt();
        }
      }
      if (mounted) {
        setState(() {
          _halls = body;
          _hallId = nextHallId;
        });
      }
      await _saveLastHallPref(nextHallId);
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

  String? _hallNameById(int? id) {
    if (id == null) return null;
    try {
      final hall = _halls.firstWhere((h) => (h['id'] as num?)?.toInt() == id);
      return hall['name']?.toString();
    } catch (_) {
      return null;
    }
  }

  @override
  void initState() {
    super.initState();
    _listenToFcm();
    _timer = Timer.periodic(const Duration(seconds: 15), (_) => _poll());
    _poll();
    _registerDevice();
    _loadLastHallPref().then((_) => _loadHalls());
  }

  Future<void> _loadLastHallPref() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final v = prefs.getInt(_lastHallPrefKey);
      if (v != null && mounted) {
        setState(() {
          _hallId = v;
          _lastHallPrefId = v;
        });
      }
    } catch (_) {}
  }

  Future<void> _saveLastHallPref(int? hallId) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      if (hallId == null) {
        await prefs.remove(_lastHallPrefKey);
      } else {
        await prefs.setInt(_lastHallPrefKey, hallId);
      }
      if (mounted) setState(() => _lastHallPrefId = hallId);
    } catch (_) {}
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
          SnackBar(content: Text('${_tr(context, 'push')}: ${message.data['type'] ?? message.notification?.title ?? _tr(context, 'notification')}')),
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
      OrdersTab(username: widget.username, password: widget.password, hallId: _hallId),
      CallsTab(username: widget.username, password: widget.password, hallId: _hallId),
      BillsTab(username: widget.username, password: widget.password, hallId: _hallId),
      KitchenTab(username: widget.username, password: widget.password, hallId: _hallId),
      FloorPlanTab(username: widget.username, password: widget.password),
      HistoryTab(username: widget.username, password: widget.password),
      NotificationsTab(events: _events),
      ProfileTab(username: widget.username, password: widget.password),
    ];
    return Scaffold(
      body: Column(
        children: [
          if (_halls.isNotEmpty)
            Container(
              padding: const EdgeInsets.fromLTRB(12, 8, 12, 0),
              child: Row(
                children: [
                  Text(_tr(context, 'hall')),
                  const SizedBox(width: 8),
                  Expanded(
                    child: DropdownButton<int>(
                      isExpanded: true,
                      value: _hallId,
                      items: _halls
                          .map((h) => DropdownMenuItem<int>(
                                value: (h['id'] as num).toInt(),
                                child: Text(h['name']?.toString() ?? _tr(context, 'hallFallback')),
                              ))
                          .toList(),
                      onChanged: (v) {
                        setState(() => _hallId = v);
                        _saveLastHallPref(v);
                      },
                    ),
                  ),
                  const SizedBox(width: 6),
                  if (_lastHallPrefId != null)
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                      decoration: BoxDecoration(
                        color: Colors.blue.withOpacity(0.1),
                        borderRadius: BorderRadius.circular(999),
                        border: Border.all(color: Colors.blue, width: 1),
                      ),
                      child: Text(
                        '${_tr(context, 'lastHallLabel')}${_hallNameById(_lastHallPrefId) ?? "${_tr(context, 'hallFallback')} #${_lastHallPrefId}"}',
                        style: const TextStyle(color: Colors.blue, fontWeight: FontWeight.w600, fontSize: 12),
                      ),
                    ),
                  if (_lastHallPrefId != null) const SizedBox(width: 6),
                  if (_lastHallPrefId != null)
                    TextButton(
                      onPressed: () async {
                        await _saveLastHallPref(null);
                        if (!mounted) return;
                        setState(() => _hallId = _halls.isNotEmpty ? (_halls.first['id'] as num).toInt() : null);
                      },
                      child: Text(_tr(context, 'resetHall')),
                    ),
                  IconButton(
                    tooltip: _tr(context, 'slaSettings'),
                    onPressed: _showSlaDialog,
                    icon: const Icon(Icons.tune),
                  ),
                ],
              ),
            ),
          Expanded(child: tabs[_index]),
        ],
      ),
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
            label: _tr(context, 'orders'),
          ),
          NavigationDestination(
            icon: _newCalls > 0 ? Badge(label: Text('$_newCalls'), child: const Icon(Icons.notifications_active)) : const Icon(Icons.notifications_active),
            label: _tr(context, 'calls'),
          ),
          NavigationDestination(
            icon: _newBills > 0 ? Badge(label: Text('$_newBills'), child: const Icon(Icons.payments)) : const Icon(Icons.payments),
            label: _tr(context, 'bills'),
          ),
          NavigationDestination(icon: const Icon(Icons.kitchen), label: _tr(context, 'kitchen')),
          NavigationDestination(icon: const Icon(Icons.map), label: _tr(context, 'hall')),
          NavigationDestination(icon: const Icon(Icons.history), label: _tr(context, 'history')),
          NavigationDestination(icon: const Icon(Icons.notifications), label: _tr(context, 'events')),
          NavigationDestination(icon: const Icon(Icons.person), label: _tr(context, 'profile')),
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
      appBar: AppBar(title: Text(_tr(context, 'events'))),
      body: events.isEmpty
          ? Center(child: Text(_tr(context, 'noEvents')))
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

class ProfileTab extends StatefulWidget {
  final String username;
  final String password;

  const ProfileTab({super.key, required this.username, required this.password});

  @override
  State<ProfileTab> createState() => _ProfileTabState();
}

class _ProfileTabState extends State<ProfileTab> {
  bool _loading = true;
  String? _error;
  Map<String, dynamic>? _profile;

  String get _auth => base64Encode(utf8.encode('${widget.username}:${widget.password}'));

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final res = await http.get(
        Uri.parse('$apiBase/api/staff/me'),
        headers: {'Authorization': 'Basic $_auth'},
      );
      if (res.statusCode != 200) throw Exception('${_tr(context, 'loadFailed')} (${res.statusCode})');
      setState(() => _profile = jsonDecode(res.body) as Map<String, dynamic>);
    } catch (e) {
      setState(() => _error = e.toString());
    } finally {
      setState(() => _loading = false);
    }
  }

  String _genderLabel(BuildContext context, String? g) {
    switch ((g ?? '').toLowerCase()) {
      case 'male':
        return _tr(context, 'genderMale');
      case 'female':
        return _tr(context, 'genderFemale');
      case 'other':
        return _tr(context, 'genderOther');
      default:
        return _tr(context, 'notSet');
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
      appBar: AppBar(title: Text(_tr(context, 'profile'))),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(child: Text(_error!))
              : _profile == null
                  ? Center(child: Text(_tr(context, 'loadFailed')))
                  : ListView(
                      padding: const EdgeInsets.all(16),
                      children: [
                        Text(_tr(context, 'myProfile'), style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w600)),
                        const SizedBox(height: 12),
                        if ((_profile?['photoUrl'] ?? '').toString().isNotEmpty)
                          Center(
                            child: ClipRRect(
                              borderRadius: BorderRadius.circular(48),
                              child: Image.network(
                                _profile?['photoUrl']?.toString() ?? '',
                                width: 96,
                                height: 96,
                                fit: BoxFit.cover,
                              ),
                            ),
                          ),
                        const SizedBox(height: 12),
                        _profileRow(context, _tr(context, 'username'), _profile?['username']?.toString()),
                        _profileRow(context, _tr(context, 'role'), _profile?['role']?.toString()),
                        _profileRow(context, _tr(context, 'firstName'), _profile?['firstName']?.toString()),
                        _profileRow(context, _tr(context, 'lastName'), _profile?['lastName']?.toString()),
                        _profileRow(context, _tr(context, 'age'), _profile?['age']?.toString()),
                        _profileRow(context, _tr(context, 'gender'), _genderLabel(context, _profile?['gender']?.toString())),
                      ],
                    ),
    );
  }

  Widget _profileRow(BuildContext context, String label, String? value) {
    final v = (value == null || value.isEmpty) ? _tr(context, 'notSet') : value;
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Row(
        children: [
          SizedBox(width: 120, child: Text(label, style: const TextStyle(color: Colors.black54))),
          Expanded(child: Text(v)),
        ],
      ),
    );
  }
}

class OrdersTab extends StatefulWidget {
  final String username;
  final String password;
  final int? hallId;

  const OrdersTab({super.key, required this.username, required this.password, this.hallId});

  @override
  State<OrdersTab> createState() => _OrdersTabState();
}

class _OrdersTabState extends State<OrdersTab> {
  bool _loading = true;
  String? _error;
  List<dynamic> _orders = const [];
  String _sortMode = 'time_desc';
  bool _showFocus = true;
  Timer? _pollTimer;
  DateTime _since = DateTime.now().toUtc();

  String get _auth => base64Encode(utf8.encode('${widget.username}:${widget.password}'));

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final res = await http.get(
        Uri.parse('$apiBase/api/staff/orders/active${widget.hallId != null ? "?hallId=${widget.hallId}" : ""}'),
        headers: {'Authorization': 'Basic $_auth'},
      );
      if (res.statusCode != 200) throw Exception('${_tr(context, 'loadFailed')} (${res.statusCode})');
      final body = jsonDecode(res.body);
      final list = body as List<dynamic>;
      setState(() => _orders = list);
      _updateSinceFromOrders(list);
    } catch (e) {
      setState(() => _error = e.toString());
    } finally {
      setState(() => _loading = false);
    }
  }

  void _updateSinceFromOrders(List<dynamic> orders) {
    DateTime? maxDt;
    for (final o in orders) {
      final dt = _parseIso(o['createdAt']?.toString());
      if (dt == null) continue;
      if (maxDt == null || dt.isAfter(maxDt)) {
        maxDt = dt;
      }
    }
    if (maxDt != null) {
      _since = maxDt.toUtc();
    }
  }

  Future<void> _pollNew() async {
    try {
      final sinceStr = _since.toUtc().toIso8601String();
      final hall = widget.hallId != null ? "&hallId=${widget.hallId}" : "";
      final res = await http.get(
        Uri.parse('$apiBase/api/staff/orders/active/updates?since=$sinceStr$hall'),
        headers: {'Authorization': 'Basic $_auth'},
      );
      if (res.statusCode != 200) return;
      final body = jsonDecode(res.body) as List<dynamic>;
      if (body.isEmpty) return;
      final Map<int, Map<String, dynamic>> byId = {
        for (final o in _orders) (o['id'] as num).toInt(): (o as Map<String, dynamic>)
      };
      for (final o in body) {
        final id = (o['id'] as num).toInt();
        byId[id] = o as Map<String, dynamic>;
      }
      final merged = byId.values.toList();
      setState(() => _orders = merged);
      _updateSinceFromOrders(body);
    } catch (_) {}
  }

  Future<void> _pollStatus() async {
    try {
      if (_orders.isEmpty) return;
      final ids = _orders.take(200).map((o) => (o['id'] as num).toInt()).join(',');
      final hall = widget.hallId != null ? "&hallId=${widget.hallId}" : "";
      final res = await http.get(
        Uri.parse('$apiBase/api/staff/orders/active/status?ids=$ids$hall'),
        headers: {'Authorization': 'Basic $_auth'},
      );
      if (res.statusCode != 200) return;
      final body = jsonDecode(res.body) as List<dynamic>;
      if (body.isEmpty) return;
      final Map<int, Map<String, dynamic>> byId = {
        for (final o in _orders) (o['id'] as num).toInt(): (o as Map<String, dynamic>)
      };
      for (final s in body) {
        final id = (s['id'] as num).toInt();
        final status = s['status']?.toString() ?? '';
        final existing = byId[id];
        if (existing != null) {
          existing['status'] = status;
        }
      }
      byId.removeWhere((_, v) {
        final status = v['status']?.toString().toUpperCase() ?? '';
        return status == 'CLOSED' || status == 'CANCELLED';
      });
      setState(() => _orders = byId.values.toList());
    } catch (_) {}
  }

  @override
  void initState() {
    super.initState();
    _load();
    _pollTimer = Timer.periodic(const Duration(seconds: 15), (_) {
      _pollNew();
      _pollStatus();
    });
  }

  @override
  void didUpdateWidget(covariant OrdersTab oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.hallId != widget.hallId) {
      _since = DateTime.now().toUtc();
      _load();
    }
  }

  @override
  void dispose() {
    _pollTimer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(_tr(context, 'orders')),
        actions: [
          IconButton(
            onPressed: () => setState(() => _showFocus = !_showFocus),
            icon: Icon(_showFocus ? Icons.visibility : Icons.visibility_off),
          ),
          PopupMenuButton<String>(
            onSelected: (v) => setState(() => _sortMode = v),
            itemBuilder: (ctx) => [
              PopupMenuItem(value: 'time_desc', child: Text(_tr(ctx, 'sortNewest'))),
              PopupMenuItem(value: 'sla_desc', child: Text(_tr(ctx, 'sortSla'))),
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
                    if (age.inMinutes >= SlaConfig.orderCrit) crit++;
                    else if (age.inMinutes >= SlaConfig.orderWarn) warn++;
                  }
                  final focusTables = maxAgeByTable.entries.toList()
                    ..sort((a, b) => b.value.inSeconds.compareTo(a.value.inSeconds));
                  final topFocus = focusTables.take(3).toList();
                  return Column(
                    children: [
                      Padding(
                        padding: const EdgeInsets.fromLTRB(16, 12, 16, 4),
                        child: _slaSummaryRow(context, warn, crit),
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
                              final color = _slaColor(age, SlaConfig.orderWarn, SlaConfig.orderCrit);
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
                                    Text('${_tr(context, 'table')} #$tableNo', style: const TextStyle(fontWeight: FontWeight.w700)),
                                    const SizedBox(height: 6),
                                    Text('${_tr(context, 'oldest')}: $minutes', style: TextStyle(color: color, fontWeight: FontWeight.w600)),
                                    const SizedBox(height: 4),
                                    Text('${_tr(context, 'ordersCount')}: $count', style: const TextStyle(color: Colors.black54)),
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
                      title: Text('${_tr(context, 'table')} #$tableNumber  •  ${_tr(context, 'orders')} #${o['id']}'),
                      subtitle: Text('${_statusLabel(context, o['status']?.toString() ?? '')} • ${items.length} ${_tr(context, 'items')}' + (assigned != null ? ' • ${_tr(context, 'waiter')} #$assigned' : '')),
                      trailing: _slaChip(age, SlaConfig.orderWarn, SlaConfig.orderCrit),
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
  final int? hallId;

  const CallsTab({super.key, required this.username, required this.password, this.hallId});

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
        Uri.parse('$apiBase/api/staff/waiter-calls/active${widget.hallId != null ? "?hallId=${widget.hallId}" : ""}'),
        headers: {'Authorization': 'Basic $_auth'},
      );
      if (res.statusCode != 200) throw Exception('${_tr(context, 'loadFailed')} (${res.statusCode})');
      final body = jsonDecode(res.body);
      setState(() => _calls = body as List<dynamic>);
    } catch (e) {
      setState(() => _error = e.toString());
    } finally {
      setState(() => _loading = false);
    }
  }

  Future<void> _setCallStatus(int id, String status) async {
    final res = await http.post(
      Uri.parse('$apiBase/api/staff/waiter-calls/$id/status'),
      headers: {'Authorization': 'Basic $_auth', 'Content-Type': 'application/json'},
      body: jsonEncode({'status': status}),
    );
    if (res.statusCode >= 300) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('${_tr(context, 'statusUpdateFailed')} (${res.statusCode})')),
      );
      return;
    }
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text('${_tr(context, 'statusUpdated')}: ${_statusLabel(context, status)}')),
    );
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
        title: Text(_tr(context, 'waiterCalls')),
        actions: [
          IconButton(
            onPressed: () => setState(() => _showFocus = !_showFocus),
            icon: Icon(_showFocus ? Icons.visibility : Icons.visibility_off),
          ),
          PopupMenuButton<String>(
            onSelected: (v) => setState(() => _sortMode = v),
            itemBuilder: (ctx) => [
              PopupMenuItem(value: 'time_desc', child: Text(_tr(ctx, 'sortNewest'))),
              PopupMenuItem(value: 'sla_desc', child: Text(_tr(ctx, 'sortSla'))),
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
                    if (age.inMinutes >= SlaConfig.callCrit) crit++;
                    else if (age.inMinutes >= SlaConfig.callWarn) warn++;
                  }
                  final focusTables = maxAgeByTable.entries.toList()
                    ..sort((a, b) => b.value.inSeconds.compareTo(a.value.inSeconds));
                  final topFocus = focusTables.take(3).toList();
                  return Column(
                    children: [
                      Padding(
                        padding: const EdgeInsets.fromLTRB(16, 12, 16, 4),
                        child: _slaSummaryRow(context, warn, crit),
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
                              final color = _slaColor(age, SlaConfig.callWarn, SlaConfig.callCrit);
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
                                    Text('${_tr(context, 'table')} #$tableNo', style: const TextStyle(fontWeight: FontWeight.w700)),
                                    const SizedBox(height: 6),
                                    Text('${_tr(context, 'oldest')}: $minutes', style: TextStyle(color: color, fontWeight: FontWeight.w600)),
                                    const SizedBox(height: 4),
                                    Text('${_tr(context, 'callsCount')}: $count', style: const TextStyle(color: Colors.black54)),
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
                            final status = (c['status'] ?? '').toString();
                            final canAck = status == 'NEW';
                            final canClose = status != 'CLOSED';
                            return ListTile(
                              title: Text('${_tr(context, 'table')} #${c['tableNumber']}'),
                              subtitle: Text('${_statusLabel(context, c['status']?.toString() ?? '')} • ${c['createdAt']}'),
                              leading: const Icon(Icons.notifications_active),
                              trailing: SizedBox(
                                width: 170,
                                child: Row(
                                  mainAxisAlignment: MainAxisAlignment.end,
                                  children: [
                                    _slaChip(age, SlaConfig.callWarn, SlaConfig.callCrit),
                                    const SizedBox(width: 8),
                                    if (canAck)
                                      OutlinedButton(
                                        onPressed: () => _setCallStatus((c['id'] as num).toInt(), 'ACKNOWLEDGED'),
                                        child: Text(_tr(context, 'ack')),
                                      ),
                                    if (canClose)
                                      Padding(
                                        padding: const EdgeInsets.only(left: 6),
                                        child: OutlinedButton(
                                          onPressed: () async {
                                            final ok = await showDialog<bool>(
                                              context: context,
                                              builder: (_) => AlertDialog(
                                                title: Text(_tr(context, 'closeCallTitle')),
                                                content: Text(_tr(context, 'closeCallBody')),
                                                actions: [
                                                  TextButton(
                                                    onPressed: () => Navigator.of(context).pop(false),
                                                    child: Text(_tr(context, 'cancel')),
                                                  ),
                                                  ElevatedButton(
                                                    onPressed: () => Navigator.of(context).pop(true),
                                                    child: Text(_tr(context, 'close')),
                                                  ),
                                                ],
                                              ),
                                            );
                                            if (ok == true) {
                                              _setCallStatus((c['id'] as num).toInt(), 'CLOSED');
                                            }
                                          },
                                          child: Text(_tr(context, 'close')),
                                        ),
                                      ),
                                  ],
                                ),
                              ),
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
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('${_tr(context, 'statusUpdateFailed')} (${res.statusCode})')));
      return;
    }
    if (!context.mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('${_tr(context, 'statusUpdated')}: ${_statusLabel(context, status)}')));
    Navigator.of(context).pop(true);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('${_tr(context, 'calls')} #${call['id']}')),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('${_tr(context, 'table')} #${call['tableNumber']}', style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 8),
            Text('${_tr(context, 'status')}: ${_statusLabel(context, call['status']?.toString() ?? '')}'),
            const SizedBox(height: 16),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton(
                    onPressed: () => _setStatus(context, 'ACKNOWLEDGED'),
                    child: Text(_tr(context, 'ack')),
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: OutlinedButton(
                    onPressed: () => _setStatus(context, 'CLOSED'),
                    child: Text(_tr(context, 'close')),
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
  final int? hallId;

  const BillsTab({super.key, required this.username, required this.password, this.hallId});

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
        Uri.parse('$apiBase/api/staff/bill-requests/active${widget.hallId != null ? "?hallId=${widget.hallId}" : ""}'),
        headers: {'Authorization': 'Basic $_auth'},
      );
      if (res.statusCode != 200) throw Exception('${_tr(context, 'loadFailed')} (${res.statusCode})');
      final body = jsonDecode(res.body);
      setState(() => _bills = body as List<dynamic>);
    } catch (e) {
      setState(() => _error = e.toString());
    } finally {
      setState(() => _loading = false);
    }
  }

  Future<void> _confirmPaid(int id) async {
    final ok = await _confirmAction(
      context,
      titleKey: 'confirm',
      contentKey: 'confirmPaidQuestion',
      confirmKey: 'confirmPaid',
    );
    if (!ok) return;
    final res = await http.post(
      Uri.parse('$apiBase/api/staff/bill-requests/$id/confirm-paid'),
      headers: {'Authorization': 'Basic $_auth'},
    );
    if (res.statusCode >= 300) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('${_tr(context, 'confirmFailed')} (${res.statusCode})')),
      );
      return;
    }
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(_tr(context, 'paymentConfirmed'))));
    _load();
  }

  Future<void> _cancelBill(int id) async {
    final ok = await _confirmAction(
      context,
      titleKey: 'confirm',
      contentKey: 'confirmCancelBill',
      confirmKey: 'cancelBill',
    );
    if (!ok) return;
    final res = await http.post(
      Uri.parse('$apiBase/api/staff/bill-requests/$id/cancel'),
      headers: {'Authorization': 'Basic $_auth'},
    );
    if (res.statusCode >= 300) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('${_tr(context, 'cancelFailed')} (${res.statusCode})')),
      );
      return;
    }
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(_tr(context, 'billCancelled'))));
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
        title: Text(_tr(context, 'billRequests')),
        actions: [
          IconButton(
            onPressed: () => setState(() => _showFocus = !_showFocus),
            icon: Icon(_showFocus ? Icons.visibility : Icons.visibility_off),
          ),
          PopupMenuButton<String>(
            onSelected: (v) => setState(() => _sortMode = v),
            itemBuilder: (ctx) => [
              PopupMenuItem(value: 'time_desc', child: Text(_tr(ctx, 'sortNewest'))),
              PopupMenuItem(value: 'sla_desc', child: Text(_tr(ctx, 'sortSla'))),
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
                    if (age.inMinutes >= SlaConfig.billCrit) crit++;
                    else if (age.inMinutes >= SlaConfig.billWarn) warn++;
                  }
                  final focusTables = maxAgeByTable.entries.toList()
                    ..sort((a, b) => b.value.inSeconds.compareTo(a.value.inSeconds));
                  final topFocus = focusTables.take(3).toList();
                  return Column(
                    children: [
                      Padding(
                        padding: const EdgeInsets.fromLTRB(16, 12, 16, 4),
                        child: _slaSummaryRow(context, warn, crit),
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
                              final color = _slaColor(age, SlaConfig.billWarn, SlaConfig.billCrit);
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
                                    Text('${_tr(context, 'table')} #$tableNo', style: const TextStyle(fontWeight: FontWeight.w700)),
                                    const SizedBox(height: 6),
                                    Text('${_tr(context, 'oldest')}: $minutes', style: TextStyle(color: color, fontWeight: FontWeight.w600)),
                                    const SizedBox(height: 4),
                                    Text('${_tr(context, 'billsCount')}: $count', style: const TextStyle(color: Colors.black54)),
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
                      title: Text('${_tr(context, 'table')} #${b['tableNumber']} • ${b['paymentMethod']}'),
                      subtitle: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text('${b['mode']} • ${b['totalCents']} ${_tr(context, 'cents')} • ${status ?? ""}'),
                          const SizedBox(height: 6),
                          _slaChip(age, SlaConfig.billWarn, SlaConfig.billCrit),
                        ],
                      ),
                      children: [
                        ...items.map((it) => ListTile(
                              title: Text('${it['name']} × ${it['qty']}'),
                              subtitle: Text('${it['lineTotalCents']} ${_tr(context, 'cents')}'),
                            )),
                        Padding(
                          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                          child: SizedBox(
                            width: double.infinity,
                              child: ElevatedButton(
                                onPressed: () => _confirmPaid(b['billRequestId']),
                                child: Text(_tr(context, 'confirmPaid')),
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
                                child: Text(_tr(context, 'cancelBill')),
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
                                  final ok = await _confirmAction(
                                    context,
                                    titleKey: 'confirm',
                                    contentKey: 'confirmCloseParty',
                                    confirmKey: 'closeParty',
                                  );
                                  if (!ok) return;
                                  final res = await http.post(
                                    Uri.parse('$apiBase/api/staff/parties/$partyId/close'),
                                    headers: {'Authorization': 'Basic $_auth'},
                                  );
                                  if (res.statusCode >= 300) {
                                    if (!context.mounted) return;
                                    ScaffoldMessenger.of(context).showSnackBar(
                                      SnackBar(content: Text('${_tr(context, 'closePartyFailed')} (${res.statusCode})')),
                                    );
                                    return;
                                  }
                                  if (!context.mounted) return;
                                  ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(_tr(context, 'partyClosed'))));
                                },
                                child: Text(_tr(context, 'closeParty')),
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
  final int? hallId;

  const KitchenTab({super.key, required this.username, required this.password, this.hallId});

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

  Future<void> _setOrderStatus(int orderId, String status) async {
    final res = await http.post(
      Uri.parse('$apiBase/api/staff/orders/$orderId/status'),
      headers: {'Authorization': 'Basic $_auth', 'Content-Type': 'application/json'},
      body: jsonEncode({'status': status}),
    );
    if (res.statusCode >= 300) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('${_tr(context, 'statusUpdateFailed')} (${res.statusCode})')),
      );
      return;
    }
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text('${_tr(context, 'statusUpdated')}: ${_statusLabel(context, status)}')),
    );
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final res = await http.get(
        Uri.parse('$apiBase/api/staff/orders/kitchen?statusIn=$_statusFilter${widget.hallId != null ? "&hallId=${widget.hallId}" : ""}'),
        headers: {'Authorization': 'Basic $_auth'},
      );
      if (res.statusCode != 200) throw Exception('${_tr(context, 'loadFailed')} (${res.statusCode})');
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
        title: Text(_tr(context, 'kitchenQueue')),
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
            itemBuilder: (ctx) => [
              PopupMenuItem(value: 'NEW,ACCEPTED,IN_PROGRESS', child: Text(_tr(ctx, 'kitchenActive'))),
              PopupMenuItem(value: 'READY', child: Text(_tr(ctx, 'kitchenReady'))),
              PopupMenuItem(value: 'NEW', child: Text(_tr(ctx, 'kitchenNewOnly'))),
            ],
          ),
          PopupMenuButton<String>(
            onSelected: (v) => setState(() => _sortMode = v),
            itemBuilder: (ctx) => [
              PopupMenuItem(value: 'time_desc', child: Text(_tr(ctx, 'sortNewest'))),
              PopupMenuItem(value: 'priority_time', child: Text(_tr(ctx, 'sortPriority'))),
              PopupMenuItem(value: 'sla_desc', child: Text(_tr(ctx, 'sortSla'))),
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
                    if (ageSec >= SlaConfig.kitchenCrit * 60) crit++;
                    else if (ageSec >= SlaConfig.kitchenWarn * 60) warn++;
                  }
                  final focusTables = maxAgeByTable.entries.toList()
                    ..sort((a, b) => b.value.inSeconds.compareTo(a.value.inSeconds));
                  final topFocus = focusTables.take(3).toList();
                  return Column(
                    children: [
                      Padding(
                        padding: const EdgeInsets.fromLTRB(16, 12, 16, 4),
                        child: _slaSummaryRow(context, warn, crit),
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
                              final color = _slaColor(age, SlaConfig.kitchenWarn, SlaConfig.kitchenCrit);
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
                                    Text('${_tr(context, 'table')} #$tableNo', style: const TextStyle(fontWeight: FontWeight.w700)),
                                    const SizedBox(height: 6),
                                    Text('${_tr(context, 'oldest')}: $minutes', style: TextStyle(color: color, fontWeight: FontWeight.w600)),
                                    const SizedBox(height: 4),
                                    Text('${_tr(context, 'kitchenCount')}: $count', style: const TextStyle(color: Colors.black54)),
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
                            final status = (o['status'] ?? '').toString().toUpperCase();
                            final orderId = (o['id'] as num).toInt();
                            final bool canServe = status == 'READY';
                            return ListTile(
                              title: Text('${_tr(context, 'table')} #$tableNumber  •  ${_tr(context, 'orders')} #${o['id']}'),
                              subtitle: Text('${_statusLabel(context, o['status']?.toString() ?? '')} • ${items.length} ${_tr(context, 'items')} • ${ageMin}m'),
                              trailing: Row(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  _slaChip(age, SlaConfig.kitchenWarn, SlaConfig.kitchenCrit),
                                  if (canServe) ...[
                                    const SizedBox(width: 8),
                                    PopupMenuButton<String>(
                                      tooltip: _tr(context, 'markServed'),
                                      onSelected: (v) async {
                                        if (v == 'CLOSED') {
                                          final ok = await _confirmAction(
                                            context,
                                            titleKey: 'confirm',
                                            contentKey: 'confirmCloseOrder',
                                            confirmKey: 'closeOrder',
                                          );
                                          if (!ok) return;
                                        }
                                        _setOrderStatus(orderId, v);
                                      },
                                      itemBuilder: (ctx) => [
                                        PopupMenuItem(value: 'SERVED', child: Text(_tr(ctx, 'markServed'))),
                                        PopupMenuItem(value: 'CLOSED', child: Text(_tr(ctx, 'closeOrder'))),
                                      ],
                                      child: const Icon(Icons.check_circle, color: Colors.green),
                                    ),
                                  ],
                                ],
                              ),
                              onTap: () {
                                Navigator.of(context).push(MaterialPageRoute(
                                  builder: (_) => OrderDetailsScreen(
                                    order: o,
                                    username: widget.username,
                                    password: widget.password,
                                    actions: const ['ACCEPTED', 'IN_PROGRESS', 'READY', 'SERVED', 'CLOSED'],
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
  Map<int, double> _heatByTableId = {};
  Map<int, String> _statusByTableId = {};
  bool _showStatusBadges = true;
  bool _showLegend = true;
  bool _useActivePlan = true;
  static const String _useActivePrefKey = 'staff_floor_use_active_plan';
  static const String _legendPrefKey = 'staff_floor_legend_visible';
  String _bgUrl = '';
  List<Map<String, dynamic>> _zones = const [];
  List<Map<String, dynamic>> _halls = const [];
  int? _hallId;
  List<Map<String, dynamic>> _plans = const [];
  int? _planId;
  Timer? _pollTimer;
  Timer? _blinkTimer;
  bool _blinkOn = true;
  final Map<String, Map<String, dynamic>> _layoutCache = {};
  bool _syncingActivePlan = false;
  static const String _lastHallPrefKey = 'staff_last_hall_id';
  int? _lastHallPrefId;

  String get _auth => base64Encode(utf8.encode('${widget.username}:${widget.password}'));

  Future<void> _loadAll({bool forceLayout = false, bool forceHalls = false}) async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      List<Map<String, dynamic>> hallsBody = _halls;
      int? hallId = _hallId;
      if (forceHalls || hallsBody.isEmpty) {
        final hallsRes = await http.get(
          Uri.parse('$apiBase/api/staff/halls'),
          headers: {'Authorization': 'Basic $_auth'},
        );
        if (hallsRes.statusCode == 200) {
          hallsBody = (jsonDecode(hallsRes.body) as List<dynamic>).cast<Map<String, dynamic>>();
        }
      }
      if (hallsBody.isNotEmpty) {
        if (hallId == null) {
          hallId = (hallsBody.first['id'] as num).toInt();
        } else {
          final exists = hallsBody.any((h) => (h['id'] as num?)?.toInt() == hallId);
          if (!exists) hallId = (hallsBody.first['id'] as num).toInt();
        }
      }
      await _saveLastHallPref(hallId);

      List<Map<String, dynamic>> plansBody = _plans;
      int? planId = _planId;
      if (hallId != null) {
        final plansRes = await http.get(
          Uri.parse('$apiBase/api/staff/halls/$hallId/plans'),
          headers: {'Authorization': 'Basic $_auth'},
        );
        if (plansRes.statusCode == 200) {
          plansBody = (jsonDecode(plansRes.body) as List<dynamic>).cast<Map<String, dynamic>>();
          if ((planId == null || _useActivePlan) && plansBody.isNotEmpty) {
            final activePlan = hallsBody.firstWhere(
              (h) => (h['id'] as num?)?.toInt() == hallId,
              orElse: () => const {},
            );
            planId = (activePlan['activePlanId'] as num?)?.toInt();
          }
        }
      }
      if (_useActivePlan) {
        _planId = planId;
      }

      final tablesRes = await http.get(
        Uri.parse('$apiBase/api/staff/tables${hallId != null ? "?hallId=$hallId" : ""}'),
        headers: {'Authorization': 'Basic $_auth'},
      );
      if (tablesRes.statusCode != 200) throw Exception('${_tr(context, 'loadTablesFailed')} (${tablesRes.statusCode})');
      final tablesBody = (jsonDecode(tablesRes.body) as List<dynamic>).cast<Map<String, dynamic>>();

      final ordersRes = await http.get(
        Uri.parse('$apiBase/api/staff/orders/active'),
        headers: {'Authorization': 'Basic $_auth'},
      );
      if (ordersRes.statusCode != 200) throw Exception('${_tr(context, 'loadOrdersFailed')} (${ordersRes.statusCode})');
      final ordersBody = (jsonDecode(ordersRes.body) as List<dynamic>).cast<Map<String, dynamic>>();
      final hot = <int>{};
      final heat = <int, double>{};
      final statusByTable = <int, String>{};
      for (final o in ordersBody) {
        final tableId = (o['tableId'] as num?)?.toInt();
        final status = (o['status'] ?? '').toString().toUpperCase();
        if (tableId != null) {
          if (status == 'NEW') {
            hot.add(tableId);
            final ageMin = _ageFromIso(o['createdAt']?.toString()).inMinutes.toDouble();
            final intensity = (ageMin / SlaConfig.orderCrit).clamp(0, 1);
            final prev = heat[tableId] ?? 0;
            if (intensity > prev) heat[tableId] = intensity;
          }
          final prevStatus = statusByTable[tableId];
          if (prevStatus == null || _statusPriority(status) > _statusPriority(prevStatus)) {
            statusByTable[tableId] = status;
          }
        }
      }

      String bgUrl = _bgUrl;
      List<Map<String, dynamic>> zones = _zones;
      if (hallId != null) {
        final cacheKey = '$hallId:${_useActivePlan ? "active" : (planId ?? "active")}';
        final cached = _layoutCache[cacheKey];
        if (cached != null && !forceLayout) {
          bgUrl = (cached['bgUrl'] ?? '').toString();
          zones = (cached['zones'] as List<Map<String, dynamic>>?) ?? const [];
        } else {
          final layoutRes = await http.get(
            Uri.parse('$apiBase/api/staff/branch-layout?hallId=$hallId${(!_useActivePlan && planId != null) ? "&planId=$planId" : ""}'),
            headers: {'Authorization': 'Basic $_auth'},
          );
          if (layoutRes.statusCode == 200) {
            final layout = jsonDecode(layoutRes.body);
            bgUrl = (layout['backgroundUrl'] ?? '').toString();
            final zonesJson = layout['zonesJson'];
            if (zonesJson != null && zonesJson.toString().isNotEmpty) {
              try {
                final parsed = jsonDecode(zonesJson.toString());
                if (parsed is List) {
                  zones = parsed.cast<Map<String, dynamic>>();
                } else {
                  zones = const [];
                }
              } catch (_) {
                zones = const [];
              }
            } else {
              zones = const [];
            }
            _layoutCache[cacheKey] = {
              'bgUrl': bgUrl,
              'zones': zones,
              'ts': DateTime.now().toIso8601String(),
            };
          }
        }
      }

      setState(() {
        _tables = tablesBody;
        _hotTableIds = hot;
        _heatByTableId = heat;
        _statusByTableId = statusByTable;
        _bgUrl = bgUrl;
        _zones = zones;
        _halls = hallsBody;
        _hallId = hallId;
        _plans = plansBody;
        if (_useActivePlan) {
          _planId = planId;
        } else {
          _planId = planId;
        }
      });
    } catch (e) {
      setState(() => _error = e.toString());
    } finally {
      setState(() => _loading = false);
    }
  }

  Future<void> _loadDynamic() async {
    try {
      if (_useActivePlan && !_syncingActivePlan) {
        final hallId = _hallId;
        if (hallId != null) {
          _syncingActivePlan = true;
          try {
            final hallsRes = await http.get(
              Uri.parse('$apiBase/api/staff/halls'),
              headers: {'Authorization': 'Basic $_auth'},
            );
            if (hallsRes.statusCode == 200) {
              final hallsBody = (jsonDecode(hallsRes.body) as List<dynamic>).cast<Map<String, dynamic>>();
              final hall = hallsBody.firstWhere(
                (h) => (h['id'] as num?)?.toInt() == hallId,
                orElse: () => const {},
              );
              final activePlanId = (hall['activePlanId'] as num?)?.toInt();
              if (activePlanId != null && activePlanId != _planId) {
                _planId = activePlanId;
                _layoutCache.remove('$hallId:active');
                await _loadAll(forceLayout: true);
                _syncingActivePlan = false;
                return;
              }
            }
          } finally {
            _syncingActivePlan = false;
          }
        }
      }
      int? hallId = _hallId;
      if (hallId == null && _halls.isNotEmpty) {
        hallId = (_halls.first['id'] as num).toInt();
      }
      final tablesRes = await http.get(
        Uri.parse('$apiBase/api/staff/tables${hallId != null ? "?hallId=$hallId" : ""}'),
        headers: {'Authorization': 'Basic $_auth'},
      );
      if (tablesRes.statusCode != 200) return;
      final tablesBody = (jsonDecode(tablesRes.body) as List<dynamic>).cast<Map<String, dynamic>>();

      final ordersRes = await http.get(
        Uri.parse('$apiBase/api/staff/orders/active'),
        headers: {'Authorization': 'Basic $_auth'},
      );
      if (ordersRes.statusCode != 200) return;
      final ordersBody = (jsonDecode(ordersRes.body) as List<dynamic>).cast<Map<String, dynamic>>();
      final hot = <int>{};
      final heat = <int, double>{};
      final statusByTable = <int, String>{};
      for (final o in ordersBody) {
        final tableId = (o['tableId'] as num?)?.toInt();
        final status = (o['status'] ?? '').toString().toUpperCase();
        if (tableId != null) {
          if (status == 'NEW') {
            hot.add(tableId);
            final ageMin = _ageFromIso(o['createdAt']?.toString()).inMinutes.toDouble();
            final intensity = (ageMin / SlaConfig.orderCrit).clamp(0, 1);
            final prev = heat[tableId] ?? 0;
            if (intensity > prev) heat[tableId] = intensity;
          }
          final prevStatus = statusByTable[tableId];
          if (prevStatus == null || _statusPriority(status) > _statusPriority(prevStatus)) {
            statusByTable[tableId] = status;
          }
        }
      }

      if (!mounted) return;
      setState(() {
        _tables = tablesBody;
        _hotTableIds = hot;
        _heatByTableId = heat;
        _statusByTableId = statusByTable;
        _hallId = hallId;
      });
    } catch (_) {}
  }

  @override
  void initState() {
    super.initState();
    _loadLegendPref();
    _loadHallPref().then((_) {
      _loadPlanPrefs();
      _loadAll(forceLayout: true, forceHalls: true);
    });
    _pollTimer = Timer.periodic(const Duration(seconds: 15), (_) => _loadDynamic());
    _blinkTimer = Timer.periodic(const Duration(milliseconds: 600), (_) {
      if (mounted) setState(() => _blinkOn = !_blinkOn);
    });
  }

  Future<void> _loadHallPref() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final v = prefs.getInt(_lastHallPrefKey);
      if (v != null && mounted) {
        setState(() {
          _hallId = v;
          _lastHallPrefId = v;
        });
      }
    } catch (_) {}
  }

  Future<void> _saveLastHallPref(int? hallId) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      if (hallId == null) {
        await prefs.remove(_lastHallPrefKey);
      } else {
        await prefs.setInt(_lastHallPrefKey, hallId);
      }
      if (mounted) setState(() => _lastHallPrefId = hallId);
    } catch (_) {}
  }

  String? _hallNameById(int? id) {
    if (id == null) return null;
    try {
      final hall = _halls.firstWhere((h) => (h['id'] as num?)?.toInt() == id);
      return hall['name']?.toString();
    } catch (_) {
      return null;
    }
  }

  Future<void> _loadLegendPref() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final v = prefs.getBool(_legendPrefKey);
      if (v != null && mounted) {
        setState(() => _showLegend = v);
      }
    } catch (_) {}
  }

  Future<void> _loadPlanPrefs() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final useActive = prefs.getBool(_useActivePrefKey);
      if (useActive != null && mounted) {
        setState(() => _useActivePlan = useActive);
      }
      if (_hallId != null) {
        final planId = prefs.getInt('staff_floor_plan_${_hallId}');
        if (planId != null && mounted) {
          setState(() => _planId = planId);
        }
      }
    } catch (_) {}
  }

  Future<void> _savePlanPref(int? planId) async {
    if (_hallId == null) return;
    try {
      final prefs = await SharedPreferences.getInstance();
      if (planId == null) {
        await prefs.remove('staff_floor_plan_${_hallId}');
      } else {
        await prefs.setInt('staff_floor_plan_${_hallId}', planId);
      }
    } catch (_) {}
  }

  Future<void> _saveUseActivePref(bool value) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setBool(_useActivePrefKey, value);
    } catch (_) {}
  }

  void _selectPlan(int? planId) {
    setState(() => _planId = planId);
    _savePlanPref(planId);
    _loadAll(forceLayout: true);
  }

  void _cyclePlan(int dir) {
    if (_useActivePlan || _plans.isEmpty) return;
    final ids = _plans
        .map((p) => (p['id'] as num?)?.toInt())
        .whereType<int>()
        .toList();
    if (ids.isEmpty) return;
    final current = _planId ?? ids.first;
    final idx = ids.indexOf(current);
    final nextIdx = idx == -1 ? 0 : (idx + dir + ids.length) % ids.length;
    _selectPlan(ids[nextIdx]);
  }

  Future<void> _saveLegendPref(bool value) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setBool(_legendPrefKey, value);
    } catch (_) {}
  }

  Future<void> _showSlaDialog() async {
    int clampInt(String v, int fallback) {
      final n = int.tryParse(v.trim());
      if (n == null || n <= 0) return fallback;
      return n;
    }

    final orderWarnCtrl = TextEditingController(text: SlaConfig.orderWarn.toString());
    final orderCritCtrl = TextEditingController(text: SlaConfig.orderCrit.toString());
    final callWarnCtrl = TextEditingController(text: SlaConfig.callWarn.toString());
    final callCritCtrl = TextEditingController(text: SlaConfig.callCrit.toString());
    final billWarnCtrl = TextEditingController(text: SlaConfig.billWarn.toString());
    final billCritCtrl = TextEditingController(text: SlaConfig.billCrit.toString());
    final kitchenWarnCtrl = TextEditingController(text: SlaConfig.kitchenWarn.toString());
    final kitchenCritCtrl = TextEditingController(text: SlaConfig.kitchenCrit.toString());

    await showDialog<void>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(_tr(ctx, 'slaSettings')),
        content: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              _slaRow(ctx, _tr(ctx, 'orders'), orderWarnCtrl, orderCritCtrl),
              _slaRow(ctx, _tr(ctx, 'calls'), callWarnCtrl, callCritCtrl),
              _slaRow(ctx, _tr(ctx, 'bills'), billWarnCtrl, billCritCtrl),
              _slaRow(ctx, _tr(ctx, 'kitchen'), kitchenWarnCtrl, kitchenCritCtrl),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () async {
              await SlaConfig.resetDefaults();
              if (!ctx.mounted) return;
              Navigator.of(ctx).pop();
              if (mounted) setState(() {});
            },
            child: Text(_tr(ctx, 'resetDefaults')),
          ),
          TextButton(onPressed: () => Navigator.of(ctx).pop(), child: Text(_tr(ctx, 'cancel'))),
          ElevatedButton(
            onPressed: () async {
              int ow = clampInt(orderWarnCtrl.text, 5);
              int oc = clampInt(orderCritCtrl.text, 10);
              int cw = clampInt(callWarnCtrl.text, 2);
              int cc = clampInt(callCritCtrl.text, 5);
              int bw = clampInt(billWarnCtrl.text, 5);
              int bc = clampInt(billCritCtrl.text, 10);
              int kw = clampInt(kitchenWarnCtrl.text, 7);
              int kc = clampInt(kitchenCritCtrl.text, 15);
              if (oc <= ow) oc = ow + 1;
              if (cc <= cw) cc = cw + 1;
              if (bc <= bw) bc = bw + 1;
              if (kc <= kw) kc = kw + 1;
              await SlaConfig.save(
                orderWarn: ow,
                orderCrit: oc,
                callWarn: cw,
                callCrit: cc,
                billWarn: bw,
                billCrit: bc,
                kitchenWarn: kw,
                kitchenCrit: kc,
              );
              if (!ctx.mounted) return;
              Navigator.of(ctx).pop();
              if (mounted) setState(() {});
            },
            child: Text(_tr(ctx, 'save')),
          ),
        ],
      ),
    );
  }

  Widget _slaRow(BuildContext ctx, String label, TextEditingController warnCtrl, TextEditingController critCtrl) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: Row(
        children: [
          Expanded(child: Text(label)),
          SizedBox(
            width: 70,
            child: TextField(
              controller: warnCtrl,
              keyboardType: TextInputType.number,
              decoration: InputDecoration(labelText: _tr(ctx, 'warn'), isDense: true),
            ),
          ),
          const SizedBox(width: 8),
          SizedBox(
            width: 70,
            child: TextField(
              controller: critCtrl,
              keyboardType: TextInputType.number,
              decoration: InputDecoration(labelText: _tr(ctx, 'crit'), isDense: true),
            ),
          ),
        ],
      ),
    );
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

  int _statusPriority(String status) {
    switch (status) {
      case 'READY':
        return 4;
      case 'IN_PROGRESS':
        return 3;
      case 'ACCEPTED':
        return 2;
      case 'NEW':
        return 1;
      default:
        return 0;
    }
  }

  Color _statusColor(String status) {
    switch (status) {
      case 'READY':
        return Colors.green.shade600;
      case 'IN_PROGRESS':
        return Colors.orange.shade600;
      case 'ACCEPTED':
        return Colors.amber.shade700;
      case 'NEW':
        return Colors.blue.shade600;
      default:
        return Colors.grey.shade400;
    }
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
        title: Text(_tr(context, 'hall')),
        actions: [
          IconButton(
            tooltip: _showStatusBadges ? _tr(context, 'hideStatusBadges') : _tr(context, 'showStatusBadges'),
            onPressed: () => setState(() => _showStatusBadges = !_showStatusBadges),
            icon: Icon(_showStatusBadges ? Icons.visibility : Icons.visibility_off),
          ),
          IconButton(
            tooltip: _tr(context, 'slaSettings'),
            onPressed: _showSlaDialog,
            icon: const Icon(Icons.tune),
          ),
          if (_halls.isNotEmpty)
            DropdownButtonHideUnderline(
              child: DropdownButton<int>(
                value: _hallId,
                items: _halls
                    .map((h) => DropdownMenuItem<int>(
                          value: (h['id'] as num).toInt(),
                          child: Text(h['name']?.toString() ?? _tr(context, 'hallFallback')),
                        ))
                    .toList(),
                onChanged: (v) {
                  setState(() {
                    _hallId = v;
                    _planId = null;
                  });
                  _saveLastHallPref(v);
                  _loadPlanPrefs();
                  _loadAll(forceLayout: true, forceHalls: true);
                },
              ),
            ),
          if (_lastHallPrefId != null)
            Padding(
              padding: const EdgeInsets.only(left: 8),
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                decoration: BoxDecoration(
                  color: Colors.blue.withOpacity(0.1),
                  borderRadius: BorderRadius.circular(999),
                  border: Border.all(color: Colors.blue, width: 1),
                ),
                child: Text(
                  '${_tr(context, 'lastHallLabel')}${_hallNameById(_lastHallPrefId) ?? "${_tr(context, 'hallFallback')} #${_lastHallPrefId}"}',
                  style: const TextStyle(color: Colors.blue, fontWeight: FontWeight.w600, fontSize: 12),
                ),
              ),
            ),
          if (_lastHallPrefId != null)
            TextButton(
              onPressed: () async {
                await _saveLastHallPref(null);
                if (!mounted) return;
                setState(() {
                  _hallId = _halls.isNotEmpty ? (_halls.first['id'] as num).toInt() : null;
                  _planId = null;
                });
                _loadPlanPrefs();
                _loadAll(forceLayout: true, forceHalls: true);
              },
              child: Text(_tr(context, 'resetHall')),
            ),
          IconButton(onPressed: () => _loadAll(forceLayout: true, forceHalls: true), icon: const Icon(Icons.refresh)),
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
      Positioned(
        right: 12,
        top: 12,
        child: GestureDetector(
          onHorizontalDragEnd: (details) {
            if (details.primaryVelocity == null) return;
            if (details.primaryVelocity! < -100) {
              _cyclePlan(1);
            } else if (details.primaryVelocity! > 100) {
              _cyclePlan(-1);
            }
          },
          child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
          decoration: BoxDecoration(
            color: Colors.white.withOpacity(0.9),
            borderRadius: BorderRadius.circular(12),
            border: Border.all(color: Colors.black12),
            boxShadow: const [BoxShadow(color: Colors.black12, blurRadius: 8, offset: Offset(0, 3))],
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
                              Row(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  Text(_tr(context, 'plans'), style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w600)),
                                  const SizedBox(width: 8),
                                  Switch(
                                    value: _useActivePlan,
                                    onChanged: (v) {
                                      setState(() {
                                        _useActivePlan = v;
                                      });
                                      _saveUseActivePref(v);
                                      _loadAll(forceLayout: true);
                                    },
                                  ),
                                  Text(_tr(context, 'useActive'), style: const TextStyle(fontSize: 11)),
                                ],
                              ),
              if (_plans.isNotEmpty) ...[
                const SizedBox(height: 6),
                Builder(builder: (context) {
                  final state = context.findAncestorStateOfType<_FloorPlanTabState>();
                  if (state == null) return const SizedBox.shrink();
                  final hall = state._halls.firstWhere(
                    (h) => (h['id'] as num?)?.toInt() == state._hallId,
                    orElse: () => const {},
                  );
                  final activePlanId = (hall['activePlanId'] as num?)?.toInt();
                  final activePlan = state._plans.firstWhere(
                    (p) => (p['id'] as num?)?.toInt() == activePlanId,
                    orElse: () => const {},
                  );
                                  return _activePlanLabel(context, activePlan, true);
                                }),
                const SizedBox(height: 8),
                Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    _planChip('Day'),
                    const SizedBox(width: 6),
                    _planChip('Evening'),
                    const SizedBox(width: 6),
                    _planChip('Banquet'),
                  ],
                ),
              ],
            ],
          ),
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
                      Positioned(
                        left: 12,
                        top: 12,
                        child: AnimatedContainer(
                          duration: const Duration(milliseconds: 200),
                          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
                          decoration: BoxDecoration(
                            color: Colors.white.withOpacity(0.9),
                            borderRadius: BorderRadius.circular(12),
                            border: Border.all(color: Colors.black12),
                            boxShadow: const [BoxShadow(color: Colors.black12, blurRadius: 8, offset: Offset(0, 3))],
                          ),
                          child: Column(
                            mainAxisSize: MainAxisSize.min,
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              InkWell(
                                onTap: () {
                                  final next = !_showLegend;
                                  setState(() => _showLegend = next);
                                  _saveLegendPref(next);
                                },
                                child: Row(
                                  mainAxisSize: MainAxisSize.min,
                                  children: [
                                    Icon(_showLegend ? Icons.expand_less : Icons.expand_more, size: 18),
                                    const SizedBox(width: 6),
                                    Text(_tr(context, 'legend'), style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w600)),
                                  ],
                                ),
                              ),
                              if (_showLegend) ...[
                                const SizedBox(height: 8),
                                Row(
                                  mainAxisSize: MainAxisSize.min,
                                  children: [
                                    _legendDot(Colors.blue.shade600),
                                    const SizedBox(width: 6),
                                    Text(_statusLabel(context, 'NEW'), style: const TextStyle(fontSize: 11)),
                                    const SizedBox(width: 10),
                                    _legendDot(Colors.amber.shade700),
                                    const SizedBox(width: 6),
                                    Text(_statusLabel(context, 'ACCEPTED'), style: const TextStyle(fontSize: 11)),
                                    const SizedBox(width: 10),
                                    _legendDot(Colors.orange.shade600),
                                    const SizedBox(width: 6),
                                    Text(_statusLabel(context, 'IN_PROGRESS'), style: const TextStyle(fontSize: 11)),
                                    const SizedBox(width: 10),
                                    _legendDot(Colors.green.shade600),
                                    const SizedBox(width: 6),
                                    Text(_statusLabel(context, 'READY'), style: const TextStyle(fontSize: 11)),
                                  ],
                                ),
                                const SizedBox(height: 8),
                                Row(
                                  mainAxisSize: MainAxisSize.min,
                                  children: [
                                    Text('${_tr(context, 'heatmap')}: ', style: const TextStyle(fontSize: 11)),
                                    _legendDot(Colors.green.shade600),
                                    const SizedBox(width: 4),
                                    Text(
                                      '${_tr(context, 'low')} (0–${SlaConfig.orderWarn}m)',
                                      style: const TextStyle(fontSize: 11),
                                    ),
                                    const SizedBox(width: 8),
                                    _legendDot(Colors.orange.shade600),
                                    const SizedBox(width: 4),
                                    Text(
                                      '${_tr(context, 'medium')} (${SlaConfig.orderWarn + 1}–${SlaConfig.orderCrit}m)',
                                      style: const TextStyle(fontSize: 11),
                                    ),
                                    const SizedBox(width: 8),
                                    _legendDot(Colors.red.shade600),
                                    const SizedBox(width: 4),
                                    Text(
                                      '${_tr(context, 'high')} (>${SlaConfig.orderCrit}m)',
                                      style: const TextStyle(fontSize: 11),
                                    ),
                                  ],
                                ),
                              ],
                            ],
                          ),
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
                        final heat = _heatByTableId[tableId] ?? 0;
                        final heatColor = Color.lerp(Colors.transparent, Colors.redAccent, heat)!;
                        final status = (_statusByTableId[tableId] ?? '').toUpperCase();
                        final statusColor = _statusColor(status);
                        final borderColor = status.isNotEmpty ? statusColor.withOpacity(0.75) : baseColor.withOpacity(0.7);

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
                                color: Color.lerp(Colors.white, heatColor.withOpacity(0.25), heat) ?? Colors.white.withOpacity(0.95),
                                borderRadius: BorderRadius.circular(shape == 'ROUND' ? 999 : 14),
                                border: Border.all(color: glow ? Colors.redAccent : borderColor, width: glow ? 2.5 : 1.4),
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
                                      waiterId == null ? _tr(context, 'unassigned') : '${_tr(context, 'waiter')} #$waiterId',
                                      style: TextStyle(color: baseColor, fontSize: 11),
                                    ),
                                    if (_showStatusBadges && status.isNotEmpty) ...[
                                      const SizedBox(height: 2),
                                      Container(
                                        padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                                        decoration: BoxDecoration(
                                          color: statusColor.withOpacity(0.12),
                                          borderRadius: BorderRadius.circular(999),
                                          border: Border.all(color: statusColor, width: 1),
                                        ),
                                        child: Text(
                                          status,
                                          style: TextStyle(color: statusColor, fontSize: 9, fontWeight: FontWeight.w700),
                                        ),
                                      ),
                                    ],
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

Widget _legendDot(Color color) {
  return Container(
    width: 10,
    height: 10,
    decoration: BoxDecoration(
      color: color,
      shape: BoxShape.circle,
    ),
  );
}

Widget _activePlanLabel(BuildContext context, Map<String, dynamic> plan, bool isActive) {
  final name = (plan['name'] ?? '').toString();
  if (name.isEmpty) return const SizedBox.shrink();
  return Container(
    padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
    decoration: BoxDecoration(
      color: isActive ? Colors.black87 : Colors.white,
      borderRadius: BorderRadius.circular(999),
      border: Border.all(color: isActive ? Colors.black87 : Colors.black12),
    ),
    child: Text(
      isActive ? '$name • ${_tr(context, 'active')}' : name,
      style: TextStyle(fontSize: 11, color: isActive ? Colors.white : Colors.black87),
    ),
  );
}

Widget _planChip(String name) {
  return Builder(builder: (context) {
    final state = context.findAncestorStateOfType<_FloorPlanTabState>();
    if (state == null) return const SizedBox.shrink();
    final plan = state._plans.firstWhere(
      (p) => (p['name'] ?? '').toString().trim().toLowerCase() == name.toLowerCase(),
      orElse: () => const {},
    );
    final planId = (plan['id'] as num?)?.toInt();
    final isActive = planId != null && planId == state._planId;
    return InkWell(
      onTap: (planId == null || state._useActivePlan)
          ? null
          : () {
              state._selectPlan(planId);
            },
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
        decoration: BoxDecoration(
          color: isActive ? Colors.black87 : (state._useActivePlan ? Colors.grey.shade200 : Colors.white),
          borderRadius: BorderRadius.circular(999),
          border: Border.all(color: isActive ? Colors.black87 : Colors.black12),
        ),
        child: Text(
          name,
          style: TextStyle(fontSize: 11, color: isActive ? Colors.white : Colors.black87),
        ),
      ),
    );
  });
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
  DateTime? _fromDate;
  DateTime? _toDate;

  String get _auth => base64Encode(utf8.encode('${widget.username}:${widget.password}'));

  Future<void> _loadTables() async {
    try {
      final res = await http.get(
        Uri.parse('$apiBase/api/staff/tables'),
        headers: {'Authorization': 'Basic $_auth'},
      );
      if (res.statusCode != 200) throw Exception('${_tr(context, 'loadTablesFailed')} (${res.statusCode})');
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
      if (res.statusCode != 200) throw Exception('${_tr(context, 'loadSessionsFailed')} (${res.statusCode})');
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
      if (_fromDate != null) {
        params.add('from=${_formatDate(_fromDate!)}');
      }
      if (_toDate != null) {
        params.add('to=${_formatDate(_toDate!)}');
      }
      final res = await http.get(
        Uri.parse('$apiBase/api/staff/orders/history?${params.join("&")}'),
        headers: {'Authorization': 'Basic $_auth'},
      );
      if (res.statusCode != 200) throw Exception('${_tr(context, 'loadHistoryFailed')} (${res.statusCode})');
      final body = (jsonDecode(res.body) as List<dynamic>).cast<Map<String, dynamic>>();
      setState(() => _orders = body);
    } catch (e) {
      setState(() => _error = e.toString());
    } finally {
      setState(() => _loading = false);
    }
  }

  String _formatDate(DateTime d) {
    final y = d.year.toString().padLeft(4, '0');
    final m = d.month.toString().padLeft(2, '0');
    final day = d.day.toString().padLeft(2, '0');
    return '$y-$m-$day';
  }

  Future<void> _pickFromDate() async {
    final picked = await showDatePicker(
      context: context,
      firstDate: DateTime(2000),
      lastDate: DateTime(2100),
      initialDate: _fromDate ?? DateTime.now(),
    );
    if (picked != null) {
      setState(() => _fromDate = picked);
      await _loadOrders();
    }
  }

  Future<void> _pickToDate() async {
    final picked = await showDatePicker(
      context: context,
      firstDate: DateTime(2000),
      lastDate: DateTime(2100),
      initialDate: _toDate ?? DateTime.now(),
    );
    if (picked != null) {
      setState(() => _toDate = picked);
      await _loadOrders();
    }
  }

  Future<void> _clearDates() async {
    setState(() {
      _fromDate = null;
      _toDate = null;
    });
    await _loadOrders();
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
        title: Text(_tr(context, 'historyTitle')),
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
                          Text('${_tr(context, 'tableLabel')}:'),
                          const SizedBox(width: 12),
                          Expanded(
                            child: DropdownButton<int?>(
                              isExpanded: true,
                              value: _selectedTableId,
                              items: _tables
                                  .map((t) => DropdownMenuItem<int?>(
                                        value: (t['id'] as num).toInt(),
                                        child: Text('${_tr(context, 'table')} #${t['number']}'),
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
                          Text('${_tr(context, 'guestLabel')}:'),
                          const SizedBox(width: 12),
                          Expanded(
                            child: DropdownButton<int?>(
                              isExpanded: true,
                              value: _selectedGuestSessionId,
                              items: [
                                DropdownMenuItem<int?>(value: null, child: Text(_tr(context, 'allGuests'))),
                                ..._sessions.map((s) {
                                  final id = (s['id'] as num).toInt();
                                  final lastOrderAt = s['lastOrderAt']?.toString();
                                  final label = lastOrderAt == null ? '${_tr(context, 'guestLabel')} #$id' : '${_tr(context, 'guestLabel')} #$id • ${_tr(context, 'lastOrder')}';
                                  return DropdownMenuItem<int?>(value: id, child: Text(label));
                                }),
                              ],
                              onChanged: (v) async {
                                setState(() => _selectedGuestSessionId = v);
                                await _loadOrders();
                              },
                            ),
                          ),
                          if (_selectedGuestSessionId != null) ...[
                            const SizedBox(width: 8),
                            OutlinedButton(
                              onPressed: () async {
                                setState(() => _selectedGuestSessionId = null);
                                await _loadOrders();
                              },
                              child: Text(_tr(context, 'back')),
                            ),
                          ],
                        ],
                      ),
                    ),
                    Padding(
                      padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            children: [
                              Text('${_tr(context, 'dateFrom')}:'),
                              const SizedBox(width: 8),
                              OutlinedButton(
                                onPressed: _pickFromDate,
                                child: Text(_fromDate == null ? _tr(context, 'selectDate') : _formatDate(_fromDate!)),
                              ),
                              const SizedBox(width: 12),
                              Text('${_tr(context, 'dateTo')}:'),
                              const SizedBox(width: 8),
                              OutlinedButton(
                                onPressed: _pickToDate,
                                child: Text(_toDate == null ? _tr(context, 'selectDate') : _formatDate(_toDate!)),
                              ),
                              const SizedBox(width: 12),
                              TextButton(onPressed: _clearDates, child: Text(_tr(context, 'clear'))),
                            ],
                          ),
                          const SizedBox(height: 8),
                          Wrap(
                            spacing: 8,
                            children: [
                              OutlinedButton(
                                onPressed: () async {
                                  final today = DateTime.now();
                                  setState(() {
                                    _fromDate = DateTime(today.year, today.month, today.day);
                                    _toDate = DateTime(today.year, today.month, today.day);
                                  });
                                  await _loadOrders();
                                },
                                child: Text(_tr(context, 'today')),
                              ),
                              OutlinedButton(
                                onPressed: () async {
                                  final now = DateTime.now();
                                  setState(() {
                                    _toDate = DateTime(now.year, now.month, now.day);
                                    _fromDate = _toDate!.subtract(const Duration(days: 6));
                                  });
                                  await _loadOrders();
                                },
                                child: Text(_tr(context, 'last7days')),
                              ),
                              OutlinedButton(
                                onPressed: () async {
                                  final now = DateTime.now();
                                  setState(() {
                                    _toDate = DateTime(now.year, now.month, now.day);
                                    _fromDate = _toDate!.subtract(const Duration(days: 29));
                                  });
                                  await _loadOrders();
                                },
                                child: Text(_tr(context, 'last30days')),
                              ),
                            ],
                          ),
                        ],
                      ),
                    ),
                    Expanded(
                      child: _orders.isEmpty
                          ? Center(child: Text(_tr(context, 'noHistory')))
                          : ListView.separated(
                              itemCount: groupKeys.length,
                              separatorBuilder: (_, __) => const Divider(height: 1),
                              itemBuilder: (ctx, i) {
                                final gs = groupKeys[i];
                                final list = groups[gs] ?? const [];
                                return ExpansionTile(
                                  title: Text('${_tr(context, 'guestLabel')} #$gs • ${list.length} ${_tr(context, 'orders')}'),
                                  children: list.map((o) {
                                    final items = (o['items'] as List<dynamic>? ?? const []).cast<Map<String, dynamic>>();
                                    return ListTile(
                                      title: Text('${_tr(context, 'order')} #${o['id']} • ${_statusLabel(context, o['status']?.toString() ?? '')}'),
                                      subtitle: Text('${o['createdAt']} • ${items.length} ${_tr(context, 'items')}'),
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
    if (status == 'CLOSED') {
      final ok = await _confirmAction(
        context,
        titleKey: 'confirm',
        contentKey: 'confirmCloseOrder',
        confirmKey: 'closeOrder',
      );
      if (!ok) return;
    }
    final id = order['id'];
    final res = await http.post(
      Uri.parse('$apiBase/api/staff/orders/$id/status'),
      headers: {'Authorization': 'Basic $_auth', 'Content-Type': 'application/json'},
      body: jsonEncode({'status': status}),
    );
    if (res.statusCode >= 300) {
      if (!context.mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('${_tr(context, 'statusUpdateFailed')} (${res.statusCode})')),
      );
      return;
    }
    if (!context.mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text('${_tr(context, 'statusUpdated')}: ${_statusLabel(context, status)}')),
    );
  }

  @override
  Widget build(BuildContext context) {
    final items = (order['items'] as List<dynamic>? ?? const []).cast<Map<String, dynamic>>();
    return Scaffold(
      appBar: AppBar(title: Text('${_tr(context, 'order')} #${order['id']}')),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('${_tr(context, 'table')} #${order['tableNumber']}', style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 8),
            Text('${_tr(context, 'status')}: ${_statusLabel(context, order['status']?.toString() ?? '')}'),
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
                              child: Text(_statusLabel(context, s)),
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
