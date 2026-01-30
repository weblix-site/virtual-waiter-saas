export type Lang = "ru" | "ro" | "en";

const dict: Record<string, Record<Lang, string>> = {
  menu: { ru: "Меню", ro: "Meniu", en: "Menu" },
  cart: { ru: "Корзина", ro: "Coș", en: "Cart" },
  cartEmpty: { ru: "Корзина пуста", ro: "Coșul e gol", en: "Cart is empty" },
  callWaiter: { ru: "Вызвать официанта", ro: "Cheamă chelnerul", en: "Call waiter" },
  createPin: { ru: "Создать PIN", ro: "Creează PIN", en: "Create PIN" },
  joinPin: { ru: "Объединиться по PIN", ro: "Conectează-te la PIN", en: "Join by PIN" },
  requestBill: { ru: "Запросить счёт", ro: "Cere nota", en: "Request bill" },
  sending: { ru: "Отправка...", ro: "Se trimite...", en: "Sending..." },
  total: { ru: "Итого", ro: "Total", en: "Total" },
  placeOrder: { ru: "Сделать заказ", ro: "Trimite comanda", en: "Place order" },
  payment: { ru: "Оплата", ro: "Plată", en: "Payment" },
  myItems: { ru: "Только моё", ro: "Doar al meu", en: "My items" },
  selected: { ru: "Выбранные", ro: "Ales", en: "Selected" },
  wholeTable: { ru: "Весь стол", ro: "Totul la masă", en: "Whole table" },
  noUnpaid: { ru: "Нет неоплаченных позиций", ro: "Nu există poziții neplătite", en: "No unpaid items" },
  cash: { ru: "Наличные", ro: "Numerar", en: "Cash" },
  terminal: { ru: "Терминал", ro: "Terminal", en: "Terminal" },
  tips: { ru: "Чаевые", ro: "Bacșiș", en: "Tips" },
  none: { ru: "Без", ro: "Fără", en: "None" },
  add: { ru: "Добавить", ro: "Adaugă", en: "Add" },
  modifiers: { ru: "Модификаторы", ro: "Modificatori", en: "Modifiers" },
  comment: { ru: "Комментарий", ro: "Comentariu", en: "Comment" },
  loading: { ru: "Загрузка...", ro: "Se încarcă...", en: "Loading..." },
  error: { ru: "Ошибка", ro: "Eroare", en: "Error" },
  details: { ru: "Подробнее", ro: "Detalii", en: "Details" },
  back: { ru: "Назад", ro: "Înapoi", en: "Back" },
};

export function t(lang: Lang, key: keyof typeof dict) {
  return dict[key][lang] ?? dict[key].ru;
}
