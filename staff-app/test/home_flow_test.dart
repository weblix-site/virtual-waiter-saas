import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:staff_app/main.dart';

class _FakeHttpOverrides extends HttpOverrides {
  _FakeHttpOverrides(this.routes);

  final Map<String, _FakeRoute> routes;

  @override
  HttpClient createHttpClient(SecurityContext? context) {
    return _FakeHttpClient(routes);
  }
}

typedef _RouteHandler = _FakeRouteResponse Function(String method, Uri uri, List<int> body);

class _FakeRouteResponse {
  final int statusCode;
  final Map<String, String> headers;
  final String body;

  const _FakeRouteResponse({required this.statusCode, this.headers = const {}, required this.body});
}

class _FakeRoute {
  final _RouteHandler handler;

  const _FakeRoute(this.handler);
}

class _FakeHttpClient implements HttpClient {
  _FakeHttpClient(this.routes);

  final Map<String, _FakeRoute> routes;

  @override
  Future<HttpClientRequest> openUrl(String method, Uri url) async {
    return _FakeHttpClientRequest(method, url, routes);
  }

  @override
  Future<HttpClientRequest> getUrl(Uri url) => openUrl('GET', url);

  @override
  Future<HttpClientRequest> postUrl(Uri url) => openUrl('POST', url);

  @override
  Future<HttpClientRequest> putUrl(Uri url) => openUrl('PUT', url);

  @override
  Future<HttpClientRequest> deleteUrl(Uri url) => openUrl('DELETE', url);

  @override
  Future<HttpClientRequest> headUrl(Uri url) => openUrl('HEAD', url);

  @override
  Future<HttpClientRequest> patchUrl(Uri url) => openUrl('PATCH', url);

  @override
  Future<HttpClientRequest> open(String method, String host, int port, String path) {
    return openUrl(method, Uri.parse('http://$host:$port$path'));
  }

  @override
  Future<HttpClientRequest> get(String host, int port, String path) => open('GET', host, port, path);

  @override
  Future<HttpClientRequest> post(String host, int port, String path) => open('POST', host, port, path);

  @override
  Future<HttpClientRequest> put(String host, int port, String path) => open('PUT', host, port, path);

  @override
  Future<HttpClientRequest> delete(String host, int port, String path) => open('DELETE', host, port, path);

  @override
  Future<HttpClientRequest> head(String host, int port, String path) => open('HEAD', host, port, path);

  @override
  Future<HttpClientRequest> patch(String host, int port, String path) => open('PATCH', host, port, path);

  @override
  void close({bool force = false}) {}

  @override
  set userAgent(String? value) {}

  @override
  String? get userAgent => 'fake';

  @override
  set autoUncompress(bool value) {}

  @override
  bool get autoUncompress => true;

  @override
  set connectionTimeout(Duration? value) {}

  @override
  Duration? get connectionTimeout => const Duration(seconds: 5);

  @override
  set idleTimeout(Duration value) {}

  @override
  Duration get idleTimeout => const Duration(seconds: 15);

  @override
  set maxConnectionsPerHost(int? value) {}

  @override
  int? get maxConnectionsPerHost => 8;

  @override
  set keyLog(Function(String line)? callback) {}

  @override
  Function(String line)? get keyLog => null;

  @override
  set findProxy(String Function(Uri url)? f) {}

  @override
  String Function(Uri url)? get findProxy => null;

  @override
  set badCertificateCallback(bool Function(X509Certificate cert, String host, int port)? callback) {}

  @override
  bool Function(X509Certificate cert, String host, int port)? get badCertificateCallback => null;

  @override
  void addCredentials(Uri url, String realm, HttpClientCredentials credentials) {}

  @override
  void addProxyCredentials(String host, int port, String realm, HttpClientCredentials credentials) {}

  @override
  set authenticate(Future<bool> Function(Uri url, String scheme, String realm)? f) {}

  @override
  Future<bool> Function(Uri url, String scheme, String realm)? get authenticate => null;

  @override
  set authenticateProxy(Future<bool> Function(String host, int port, String scheme, String realm)? f) {}

  @override
  Future<bool> Function(String host, int port, String scheme, String realm)? get authenticateProxy => null;

  @override
  set proxyAuthenticationRequired(Future<bool> Function(String host, int port, String scheme, String realm)? f) {}

  @override
  Future<bool> Function(String host, int port, String scheme, String realm)? get proxyAuthenticationRequired => null;
}

class _FakeHttpClientRequest implements HttpClientRequest {
  _FakeHttpClientRequest(this._method, this.uri, this.routes);

  final String _method;
  @override
  final Uri uri;
  final Map<String, _FakeRoute> routes;
  final _headers = _FakeHttpHeaders();
  final _body = BytesBuilder();

  @override
  HttpHeaders get headers => _headers;

  @override
  Encoding get encoding => utf8;

  @override
  set encoding(Encoding value) {}

  @override
  int get contentLength => _body.length;

  @override
  set contentLength(int value) {}

  @override
  bool get followRedirects => false;

  @override
  set followRedirects(bool value) {}

  @override
  int get maxRedirects => 5;

  @override
  set maxRedirects(int value) {}

  @override
  bool get persistentConnection => true;

  @override
  set persistentConnection(bool value) {}

  @override
  void add(List<int> data) => _body.add(data);

  @override
  void addError(Object error, [StackTrace? stackTrace]) {}

  @override
  Future addStream(Stream<List<int>> stream) async {
    await for (final chunk in stream) {
      _body.add(chunk);
    }
  }

  @override
  void write(Object? obj) {
    if (obj == null) return;
    _body.add(utf8.encode(obj.toString()));
  }

  @override
  void writeAll(Iterable objects, [String separator = ""]) {
    write(objects.join(separator));
  }

  @override
  void writeCharCode(int charCode) {
    _body.add([charCode]);
  }

  @override
  void writeln([Object? obj = ""]) {
    write(obj);
    write("\n");
  }

  @override
  Future<HttpClientResponse> close() async {
    final key = uri.path;
    final route = routes[key];
    if (route == null) {
      return _FakeHttpClientResponse(200, '[]');
    }
    final res = route.handler(_method, uri, _body.takeBytes());
    return _FakeHttpClientResponse(res.statusCode, res.body, res.headers);
  }

  @override
  void abort([Object? exception, StackTrace? stackTrace]) {}

  @override
  Future<HttpClientResponse> get done => close();

  @override
  bool bufferOutput = true;

  @override
  void flush() {}

  @override
  String get method => _method;

  @override
  HttpConnectionInfo? get connectionInfo => null;

  @override
  List<Cookie> get cookies => const [];
}

class _FakeHttpClientResponse extends Stream<List<int>> implements HttpClientResponse {
  _FakeHttpClientResponse(this.statusCode, String body, [Map<String, String> headers = const {}])
      : _bodyBytes = utf8.encode(body),
        _headers = _FakeHttpHeaders.from(headers);

  final List<int> _bodyBytes;
  final _FakeHttpHeaders _headers;

  @override
  final int statusCode;

  @override
  HttpHeaders get headers => _headers;

  @override
  int get contentLength => _bodyBytes.length;

  @override
  String get reasonPhrase => 'OK';

  @override
  bool get isRedirect => false;

  @override
  bool get persistentConnection => false;

  @override
  List<RedirectInfo> get redirects => const [];

  @override
  Future<Socket> detachSocket() {
    throw UnimplementedError();
  }

  @override
  X509Certificate? get certificate => null;

  @override
  HttpConnectionInfo? get connectionInfo => null;

  @override
  CompressionState get compressionState => CompressionState.notCompressed;

  @override
  List<Cookie> get cookies => const [];

  @override
  StreamSubscription<List<int>> listen(void Function(List<int>)? onData, {Function? onError, void Function()? onDone, bool? cancelOnError}) {
    return Stream<List<int>>.fromIterable([_bodyBytes]).listen(onData, onError: onError, onDone: onDone, cancelOnError: cancelOnError);
  }
}

class _FakeHttpHeaders implements HttpHeaders {
  _FakeHttpHeaders();

  _FakeHttpHeaders.from(Map<String, String> values) {
    values.forEach((key, value) => set(key, value));
  }

  final Map<String, List<String>> _values = {};

  @override
  List<String>? operator [](String name) => _values[name.toLowerCase()];

  @override
  void add(String name, Object value, {bool preserveHeaderCase = false}) {
    final key = name.toLowerCase();
    _values.putIfAbsent(key, () => []).add(value.toString());
  }

  @override
  void set(String name, Object value, {bool preserveHeaderCase = false}) {
    _values[name.toLowerCase()] = [value.toString()];
  }

  @override
  void remove(String name, Object value) {
    final key = name.toLowerCase();
    _values[key]?.remove(value.toString());
  }

  @override
  void removeAll(String name) {
    _values.remove(name.toLowerCase());
  }

  @override
  void forEach(void Function(String name, List<String> values) action) {
    _values.forEach(action);
  }

  @override
  void noFolding(String name) {}

  @override
  void clear() => _values.clear();

  @override
  String? value(String name) {
    final v = _values[name.toLowerCase()];
    if (v == null || v.isEmpty) return null;
    if (v.length == 1) return v.first;
    return v.join(',');
  }

  @override
  DateTime? date;

  @override
  DateTime? expires;

  @override
  String? host;

  @override
  int? port;

  @override
  ContentType? contentType;

  @override
  int? contentLength;

  @override
  bool chunkedTransferEncoding = false;

  @override
  bool persistentConnection = true;

  @override
  List<Cookie> get cookies => const [];

  @override
  set ifModifiedSince(DateTime? ifModifiedSince) {}

  @override
  DateTime? get ifModifiedSince => null;

  @override
  void setDate(DateTime date) {}

  @override
  void setExpires(DateTime expires) {}

  @override
  void addAll(String name, Iterable<Object> values, {bool preserveHeaderCase = false}) {
    for (final v in values) {
      add(name, v, preserveHeaderCase: preserveHeaderCase);
    }
  }

  @override
  void setAll(String name, Iterable<Object> values, {bool preserveHeaderCase = false}) {
    _values[name.toLowerCase()] = values.map((e) => e.toString()).toList();
  }

  @override
  void removeAllValues(String name, Iterable<Object> values) {
    final key = name.toLowerCase();
    final list = _values[key];
    if (list == null) return;
    for (final v in values) {
      list.remove(v.toString());
    }
  }

  @override
  void setDate(DateTime date) {}

  @override
  void setExpires(DateTime expires) {}
}

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
    SharedPreferences.setMockInitialValues({});
  });

  tearDown(() {
    binding.window.clearPhysicalSizeTestValue();
    binding.window.clearDevicePixelRatioTestValue();
    HttpOverrides.global = null;
  });

  testWidgets('login flow opens home screen', (tester) async {
    final routes = <String, _FakeRoute>{
      '/api/staff/me': _FakeRoute((_, __, ___) => const _FakeRouteResponse(statusCode: 200, body: '{}')),
      '/api/staff/halls': _FakeRoute((_, __, ___) => const _FakeRouteResponse(statusCode: 200, body: '[]')),
      '/api/staff/notifications/feed': _FakeRoute((_, __, ___) => const _FakeRouteResponse(statusCode: 200, body: '{"lastId":0,"events":[]}')),
      '/api/staff/notifications': _FakeRoute((_, __, ___) => const _FakeRouteResponse(statusCode: 200, body: '{"newOrders":0,"newCalls":0,"newBills":0}')),
      '/api/staff/chat/threads': _FakeRoute((_, __, ___) => const _FakeRouteResponse(statusCode: 200, body: '[]')),
      '/api/staff/orders/active': _FakeRoute((_, __, ___) => const _FakeRouteResponse(statusCode: 200, body: '[]')),
      '/api/staff/orders/active/updates': _FakeRoute((_, __, ___) => const _FakeRouteResponse(statusCode: 200, body: '[]')),
      '/api/staff/orders/active/status': _FakeRoute((_, __, ___) => const _FakeRouteResponse(statusCode: 200, body: '[]')),
      '/api/staff/devices/register': _FakeRoute((_, __, ___) => const _FakeRouteResponse(statusCode: 200, body: '{}')),
    };
    HttpOverrides.global = _FakeHttpOverrides(routes);

    await tester.pumpWidget(_wrapApp(const LoginScreen(), const Locale('en')));
    await tester.tap(find.text('Login'));
    await tester.pumpAndSettle(const Duration(milliseconds: 500));

    expect(find.text('Orders'), findsOneWidget);
    expect(find.byType(NavigationBar), findsOneWidget);
  });

  testWidgets('hall selector and last hall label render', (tester) async {
    SharedPreferences.setMockInitialValues({'staff_last_hall_id': 2});
    final routes = <String, _FakeRoute>{
      '/api/staff/me': _FakeRoute((_, __, ___) => const _FakeRouteResponse(statusCode: 200, body: '{}')),
      '/api/staff/halls': _FakeRoute((_, __, ___) => const _FakeRouteResponse(statusCode: 200, body: '[{"id":1,"name":"Hall A"},{"id":2,"name":"Hall B"}]')),
      '/api/staff/notifications/feed': _FakeRoute((_, __, ___) => const _FakeRouteResponse(statusCode: 200, body: '{"lastId":0,"events":[]}')),
      '/api/staff/notifications': _FakeRoute((_, __, ___) => const _FakeRouteResponse(statusCode: 200, body: '{"newOrders":0,"newCalls":0,"newBills":0}')),
      '/api/staff/chat/threads': _FakeRoute((_, __, ___) => const _FakeRouteResponse(statusCode: 200, body: '[]')),
      '/api/staff/orders/active': _FakeRoute((_, __, ___) => const _FakeRouteResponse(statusCode: 200, body: '[]')),
      '/api/staff/orders/active/updates': _FakeRoute((_, __, ___) => const _FakeRouteResponse(statusCode: 200, body: '[]')),
      '/api/staff/orders/active/status': _FakeRoute((_, __, ___) => const _FakeRouteResponse(statusCode: 200, body: '[]')),
      '/api/staff/devices/register': _FakeRoute((_, __, ___) => const _FakeRouteResponse(statusCode: 200, body: '{}')),
    };
    HttpOverrides.global = _FakeHttpOverrides(routes);

    await tester.pumpWidget(_wrapApp(const LoginScreen(), const Locale('en')));
    await tester.tap(find.text('Login'));
    await tester.pumpAndSettle(const Duration(milliseconds: 500));

    expect(find.text('Hall A'), findsOneWidget);
    expect(find.textContaining('Last hall:'), findsOneWidget);
  });

  testWidgets('notifications feed shows events', (tester) async {
    final routes = <String, _FakeRoute>{
      '/api/staff/me': _FakeRoute((_, __, ___) => const _FakeRouteResponse(statusCode: 200, body: '{}')),
      '/api/staff/halls': _FakeRoute((_, __, ___) => const _FakeRouteResponse(statusCode: 200, body: '[{"id":1,"name":"Hall A"}]')),
      '/api/staff/notifications/feed': _FakeRoute((_, __, ___) => const _FakeRouteResponse(statusCode: 200, body: '{"lastId":1,"events":[{"type":"ORDER_NEW","refId":101,"createdAt":"2026-01-01T00:00:00Z"}]}')),
      '/api/staff/notifications': _FakeRoute((_, __, ___) => const _FakeRouteResponse(statusCode: 200, body: '{"newOrders":2,"newCalls":1,"newBills":0}')),
      '/api/staff/chat/threads': _FakeRoute((_, __, ___) => const _FakeRouteResponse(statusCode: 200, body: '[]')),
      '/api/staff/orders/active': _FakeRoute((_, __, ___) => const _FakeRouteResponse(statusCode: 200, body: '[]')),
      '/api/staff/orders/active/updates': _FakeRoute((_, __, ___) => const _FakeRouteResponse(statusCode: 200, body: '[]')),
      '/api/staff/orders/active/status': _FakeRoute((_, __, ___) => const _FakeRouteResponse(statusCode: 200, body: '[]')),
      '/api/staff/devices/register': _FakeRoute((_, __, ___) => const _FakeRouteResponse(statusCode: 200, body: '{}')),
    };
    HttpOverrides.global = _FakeHttpOverrides(routes);

    await tester.pumpWidget(_wrapApp(const LoginScreen(), const Locale('en')));
    await tester.tap(find.text('Login'));
    await tester.pumpAndSettle(const Duration(milliseconds: 500));

    await tester.tap(find.text('Events'));
    await tester.pumpAndSettle(const Duration(milliseconds: 300));

    expect(find.textContaining('ORDER_NEW'), findsOneWidget);
    expect(find.textContaining('#101'), findsOneWidget);
  });

  testWidgets('navigate to calls tab shows waiter calls screen', (tester) async {
    final routes = <String, _FakeRoute>{
      '/api/staff/me': _FakeRoute((_, __, ___) => const _FakeRouteResponse(statusCode: 200, body: '{}')),
      '/api/staff/halls': _FakeRoute((_, __, ___) => const _FakeRouteResponse(statusCode: 200, body: '[{\"id\":1,\"name\":\"Hall A\"}]')),
      '/api/staff/notifications/feed': _FakeRoute((_, __, ___) => const _FakeRouteResponse(statusCode: 200, body: '{\"lastId\":0,\"events\":[]}')),
      '/api/staff/notifications': _FakeRoute((_, __, ___) => const _FakeRouteResponse(statusCode: 200, body: '{\"newOrders\":0,\"newCalls\":0,\"newBills\":0}')),
      '/api/staff/chat/threads': _FakeRoute((_, __, ___) => const _FakeRouteResponse(statusCode: 200, body: '[]')),
      '/api/staff/orders/active': _FakeRoute((_, __, ___) => const _FakeRouteResponse(statusCode: 200, body: '[]')),
      '/api/staff/orders/active/updates': _FakeRoute((_, __, ___) => const _FakeRouteResponse(statusCode: 200, body: '[]')),
      '/api/staff/orders/active/status': _FakeRoute((_, __, ___) => const _FakeRouteResponse(statusCode: 200, body: '[]')),
      '/api/staff/waiter-calls/active': _FakeRoute((_, __, ___) => const _FakeRouteResponse(statusCode: 200, body: '[]')),
      '/api/staff/devices/register': _FakeRoute((_, __, ___) => const _FakeRouteResponse(statusCode: 200, body: '{}')),
    };
    HttpOverrides.global = _FakeHttpOverrides(routes);

    await tester.pumpWidget(_wrapApp(const LoginScreen(), const Locale('en')));
    await tester.tap(find.text('Login'));
    await tester.pumpAndSettle(const Duration(milliseconds: 500));

    await tester.tap(find.text('Calls'));
    await tester.pumpAndSettle(const Duration(milliseconds: 300));

    expect(find.text('Waiter Calls'), findsOneWidget);
  });

  testWidgets('navigate to chat tab shows chat screen', (tester) async {
    final routes = <String, _FakeRoute>{
      '/api/staff/me': _FakeRoute((_, __, ___) => const _FakeRouteResponse(statusCode: 200, body: '{}')),
      '/api/staff/halls': _FakeRoute((_, __, ___) => const _FakeRouteResponse(statusCode: 200, body: '[{\"id\":1,\"name\":\"Hall A\"}]')),
      '/api/staff/notifications/feed': _FakeRoute((_, __, ___) => const _FakeRouteResponse(statusCode: 200, body: '{\"lastId\":0,\"events\":[]}')),
      '/api/staff/notifications': _FakeRoute((_, __, ___) => const _FakeRouteResponse(statusCode: 200, body: '{\"newOrders\":0,\"newCalls\":0,\"newBills\":0}')),
      '/api/staff/chat/threads': _FakeRoute((_, __, ___) => const _FakeRouteResponse(statusCode: 200, body: '[]')),
      '/api/staff/orders/active': _FakeRoute((_, __, ___) => const _FakeRouteResponse(statusCode: 200, body: '[]')),
      '/api/staff/orders/active/updates': _FakeRoute((_, __, ___) => const _FakeRouteResponse(statusCode: 200, body: '[]')),
      '/api/staff/orders/active/status': _FakeRoute((_, __, ___) => const _FakeRouteResponse(statusCode: 200, body: '[]')),
      '/api/staff/devices/register': _FakeRoute((_, __, ___) => const _FakeRouteResponse(statusCode: 200, body: '{}')),
    };
    HttpOverrides.global = _FakeHttpOverrides(routes);

    await tester.pumpWidget(_wrapApp(const LoginScreen(), const Locale('en')));
    await tester.tap(find.text('Login'));
    await tester.pumpAndSettle(const Duration(milliseconds: 500));

    await tester.tap(find.text('Chat'));
    await tester.pumpAndSettle(const Duration(milliseconds: 300));

    expect(find.text('Chat'), findsOneWidget);
  });
}
