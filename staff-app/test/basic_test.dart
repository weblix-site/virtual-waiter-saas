import 'package:flutter_test/flutter_test.dart';
import 'package:staff_app/main.dart';

void main() {
  testWidgets('renders login screen (EN)', (tester) async {
    await tester.pumpWidget(const MaterialApp(
      locale: Locale('en'),
      home: LoginScreen(),
    ));
    expect(find.text('Staff Login'), findsOneWidget);
    expect(find.text('Demo: waiter1 / demo123'), findsOneWidget);
    expect(find.text('Username'), findsOneWidget);
    expect(find.text('Password'), findsOneWidget);
    expect(find.text('Login'), findsOneWidget);
  });

  testWidgets('renders login screen (RU)', (tester) async {
    await tester.pumpWidget(const MaterialApp(
      locale: Locale('ru'),
      home: LoginScreen(),
    ));
    expect(find.text('Вход персонала'), findsOneWidget);
    expect(find.text('Демо: waiter1 / demo123'), findsOneWidget);
    expect(find.text('Логин'), findsOneWidget);
    expect(find.text('Пароль'), findsOneWidget);
    expect(find.text('Войти'), findsOneWidget);
  });

  testWidgets('password field is obscured and defaults are set', (tester) async {
    await tester.pumpWidget(const MaterialApp(
      locale: Locale('en'),
      home: LoginScreen(),
    ));
    final fields = tester.widgetList<TextField>(find.byType(TextField)).toList();
    expect(fields.length, 2);
    expect(fields[1].obscureText, isTrue);
    expect(fields[0].controller?.text, 'waiter1');
    expect(fields[1].controller?.text, 'demo123');
  });
}
