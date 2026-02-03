import 'package:flutter_test/flutter_test.dart';
import 'package:staff_app/main.dart';

void main() {
  testWidgets('renders login screen', (tester) async {
    await tester.pumpWidget(const StaffApp());
    expect(find.text('Staff Login'), findsOneWidget);
    expect(find.text('Demo: waiter1 / demo123'), findsOneWidget);
  });
}
