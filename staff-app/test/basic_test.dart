import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:staff_app/main.dart';

Widget _wrapApp(Widget child, Locale locale) {
  return MaterialApp(
    locale: locale,
    supportedLocales: const [
      Locale('en'),
      Locale('ru'),
      Locale('ro'),
    ],
    localizationsDelegates: const [
      GlobalMaterialLocalizations.delegate,
      GlobalWidgetsLocalizations.delegate,
      GlobalCupertinoLocalizations.delegate,
    ],
    home: child,
  );
}

void main() {
  final binding = TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    binding.window.physicalSizeTestValue = const Size(1200, 2000);
    binding.window.devicePixelRatioTestValue = 1.0;
  });

  tearDown(() {
    binding.window.clearPhysicalSizeTestValue();
    binding.window.clearDevicePixelRatioTestValue();
  });

  testWidgets('renders login screen (EN)', (tester) async {
    await tester.pumpWidget(_wrapApp(const LoginScreen(), const Locale('en')));
    expect(find.text('Staff Login'), findsOneWidget);
    expect(find.text('Demo: waiter1 / demo123'), findsOneWidget);
    expect(find.text('Username'), findsOneWidget);
    expect(find.text('Password'), findsOneWidget);
    expect(find.text('Login'), findsOneWidget);
  });

  testWidgets('renders login screen (RU)', (tester) async {
    await tester.pumpWidget(_wrapApp(const LoginScreen(), const Locale('ru')));
    expect(find.text('Вход персонала'), findsOneWidget);
    expect(find.text('Демо: waiter1 / demo123'), findsOneWidget);
    expect(find.text('Логин'), findsOneWidget);
    expect(find.text('Пароль'), findsOneWidget);
    expect(find.text('Войти'), findsOneWidget);
  });

  testWidgets('password field is obscured and defaults are set', (tester) async {
    await tester.pumpWidget(_wrapApp(const LoginScreen(), const Locale('en')));
    final fields = tester.widgetList<TextField>(find.byType(TextField)).toList();
    expect(fields.length, 2);
    expect(fields[1].obscureText, isTrue);
    expect(fields[0].controller?.text, 'waiter1');
    expect(fields[1].controller?.text, 'demo123');
  });
}
