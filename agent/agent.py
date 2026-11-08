import argparse
import json
import math
import os
import re
import sys
import time
from datetime import datetime
from pathlib import Path

from dotenv import load_dotenv
from openai import OpenAI, RateLimitError, InternalServerError

load_dotenv(Path(__file__).parent / ".env")

client = OpenAI(
    api_key=os.getenv("OPENAI_API_KEY"),
    base_url=os.getenv("OPENAI_BASE_URL"),
    timeout=120,
)
MODEL = os.getenv("OPENAI_MODEL")

WORKSPACE = (Path(__file__).parent / "workspace").resolve()
MAX_STEPS = 10

# Лаб. 3, шаг 3: бюджет контекста в «условных токенах» (оценка len(text) // 4).
# Если оценка входа больше бюджета, старая часть истории удаляется.
CONTEXT_BUDGET = int(os.getenv("CONTEXT_BUDGET", "6000"))
# До скольких символов сокращать старые результаты инструментов, если не помогло удаление ходов.
TOOL_RESULT_KEEP = 400
SHORTENED_MARK = "…[сокращено"  # пометка, чтобы не сокращать один результат дважды

# Лаб. 3, шаг 4: что делать со старыми ходами при превышении бюджета.
#   trim      — просто удалить;
#   summarize — сжать отдельным запросом к модели в одно сообщение «[память диалога] …».
STRATEGY = os.getenv("STRATEGY", "summarize").lower()
MEMORY_PREFIX = "[память диалога]"
SUMMARY_RESERVE = 300   # сколько токенов бюджета оставить под саму память
SUMMARY_MAX_CHARS = 1200

# Лаб. 3, шаг 1: сохранённые диалоги (краткосрочная память между запусками)
SESSIONS_DIR = (Path(__file__).parent / "sessions").resolve()

# данные проектных инструментов (бюджет и расходы поездки)
TRAVEL_FILE = WORKSPACE / "travel.json"
CATEGORIES = ["еда", "транспорт", "жильё", "билеты", "покупки", "другое"]

SYSTEM_PROMPT = (
    "Ты агент проекта Travel Planner. Отвечай кратко, по-русски. "
    "Если сообщение не требует работы с файлами, документацией или бюджетом "
    "(знакомство, вопрос о том, что говорилось раньше), отвечай сразу, без инструментов. "
    "Ты работаешь только с файлами внутри workspace. "
    "Не придумывай содержимое файлов: чтобы узнать, какие файлы есть, "
    "вызывай list_files, чтобы прочитать файл, вызывай read_file. "
    "Чтобы создать или изменить файл, вызывай write_file — "
    "пользователь должен подтвердить запись. Если запись отклонена, "
    "не пытайся записать снова без новой просьбы пользователя. "
    "На вопросы о том, как устроено приложение Travel Planner, сначала ищи ответ "
    "в документации через search_docs, а не по памяти. "
    "Для учёта расходов поездки используй add_expense, set_budget "
    "и budget_status. Суммы — в рублях. Не считай остаток сам, "
    "всегда вызывай budget_status."
)

# ---------- безопасный путь ----------

def safe_path(path):
    """Превращает путь от модели в абсолютный и проверяет, что он внутри workspace."""
    full = (WORKSPACE / path).resolve()
    if not full.is_relative_to(WORKSPACE):
        raise PermissionError(f"доступ за пределы workspace запрещён: {path}")
    return full


# ---------- инструменты ----------

def list_files():
    files = [str(p.relative_to(WORKSPACE)) for p in WORKSPACE.rglob("*") if p.is_file()]
    return "\n".join(files) if files else "Файлов нет."


def read_file(path):
    full = safe_path(path)
    if not full.is_file():
        raise FileNotFoundError(f"файл '{path}' не найден")
    return full.read_text(encoding="utf-8")


def write_file(path, content):
    full = safe_path(path)
    print(f"\n  Агент хочет записать файл: {path} ({len(content)} символов)")
    print("  " + content[:300].replace("\n", "\n  "))
    answer = logged_input("  Разрешить? [y/N] ")
    if answer.strip().lower() != "y":
        return "ОТКАЗ: пользователь запретил запись файла."
    full.parent.mkdir(parents=True, exist_ok=True)
    full.write_text(content, encoding="utf-8")
    return f"Файл '{path}' записан ({len(content)} символов)."


# ---------- поиск по документации (лаб. 3, шаг 5) ----------

DOCS_DIR = WORKSPACE / "docs"
SNIPPET_CHARS = 400  # длина одного фрагмента в ответе — без «гигантского дампа»

# Частые слова, которые ничего не говорят о теме запроса.
STOP_WORDS = {
    "и", "в", "во", "на", "по", "с", "со", "к", "ко", "у", "о", "об", "от", "до", "из", "за",
    "для", "при", "не", "ли", "а", "но", "или", "как", "что", "это", "есть", "какой", "какая",
    "какое", "какие", "каким", "где", "когда", "мне", "мой", "моя", "мои", "найди", "найти",
    "покажи", "скажи", "сколько", "можно", "ли", "документации", "документация", "документ", "приложение", "приложении",
}


def tokenize(text):
    return re.findall(r"[a-zа-яё0-9]+", text.lower())


def stem(word):
    """Очень простой «стемминг» для русского: отрезаем окончание (до 2 букв)
    и оставляем не больше 6 букв основы.
    «бюджет» -> «бюдж» найдёт «бюджета» и «бюджетом»;
    «обновляются» -> «обновл» найдёт «обновляет»."""
    return word if len(word) <= 4 else word[:min(6, max(4, len(word) - 2))]


def doc_paragraphs():
    """Все абзацы из .md/.txt в docs/: (путь, заголовок раздела, текст абзаца)."""
    docs = safe_path("docs")  # та же песочница, что у read_file
    if not docs.is_dir():
        raise FileNotFoundError("папка workspace/docs не найдена — положите туда документы проекта")
    for file in sorted(docs.rglob("*")):
        full = file.resolve()
        if file.suffix.lower() not in (".md", ".txt") or not full.is_relative_to(docs):
            continue  # только тексты и только внутри docs/ (защита от симлинков наружу)
        heading = ""
        for block in re.split(r"\n\s*\n", full.read_text(encoding="utf-8")):
            block = block.strip()
            if not block:
                continue
            if block.startswith("#"):
                heading = block.lstrip("#").strip().split("\n")[0]
                rest = "\n".join(block.split("\n")[1:]).strip()
                if not rest:
                    continue  # абзац из одного заголовка — запоминаем как раздел
                block = rest
            yield str(full.relative_to(WORKSPACE)), heading, block


def search_docs(query, limit=3):
    """Keyword-поиск по абзацам docs/: регистр не важен, query разбивается на слова.
    Для каждого найденного слова запроса: score += idf × (1 + 0.5, если слово в заголовке)
    + 0.1 за каждое повторное вхождение. idf — вес редкости: слово, которое есть почти
    в каждом абзаце («бюджет»), весит меньше редкого («демо»)."""
    limit = int(limit)
    if not 1 <= limit <= 5:
        raise ValueError("limit должен быть от 1 до 5")
    stems = {stem(w) for w in tokenize(query) if w not in STOP_WORDS and len(w) > 1}
    if not stems:
        raise ValueError("пустой запрос: укажите ключевые слова, например «бюджет демо-поездки»")

    paragraphs = [(path, heading, text, tokenize(text), tokenize(heading))
                  for path, heading, text in doc_paragraphs()]
    # df — в скольких абзацах встречается слово; idf = ln((N + 1) / (df + 1)) + 1
    n = len(paragraphs)
    idf = {}
    for st in stems:
        df = sum(any(w.startswith(st) for w in words + title) for _, _, _, words, title in paragraphs)
        idf[st] = math.log((n + 1) / (df + 1)) + 1

    results = []
    for path, heading, text, words, title_words in paragraphs:
        score = 0.0
        for st in stems:
            hits = sum(w.startswith(st) for w in words)
            in_title = any(w.startswith(st) for w in title_words)
            if hits or in_title:
                score += idf[st] * (1 + (0.5 if in_title else 0)) + 0.1 * max(hits - 1, 0)
        if score:
            results.append((score, path, heading, text))

    if not results:
        return f"По запросу «{query}» в docs ничего не найдено. Попробуйте другие слова."
    results.sort(key=lambda r: -r[0])
    lines = [f"Найдено фрагментов: {len(results)}, показаны лучшие {min(limit, len(results))}."]
    for score, path, heading, text in results[:limit]:
        snippet = " ".join(text.split())
        if len(snippet) > SNIPPET_CHARS:
            snippet = snippet[:SNIPPET_CHARS] + "…"
        lines.append(f"\n[{path} › {heading}] (score {score:.1f})\n{snippet}")
    return "\n".join(lines)


# ---------- проектные инструменты Travel Planner ----------

def load_travel():
    """Читает travel.json; если файла нет — возвращает пустую поездку."""
    if TRAVEL_FILE.exists():
        return json.loads(TRAVEL_FILE.read_text(encoding="utf-8"))
    return {"budget": 0, "expenses": []}


def save_travel(data):
    TRAVEL_FILE.write_text(json.dumps(data, ensure_ascii=False, indent=2),
                           encoding="utf-8")


def set_budget(amount):
    amount = float(amount)
    if amount <= 0:
        raise ValueError("бюджет должен быть больше 0")
    data = load_travel()
    data["budget"] = amount
    save_travel(data)
    return f"Бюджет поездки: {amount:.0f} руб."


def add_expense(amount, category, note=""):
    amount = float(amount)
    if amount <= 0:
        raise ValueError("сумма расхода должна быть больше 0")
    if category not in CATEGORIES:
        raise ValueError(f"неизвестная категория '{category}', "
                         f"допустимые: {', '.join(CATEGORIES)}")
    data = load_travel()
    data["expenses"].append({"amount": amount, "category": category, "note": note})
    save_travel(data)
    return f"Добавлен расход {amount:.0f} руб. ({category}{', ' + note if note else ''})."


def budget_status(category=None):
    data = load_travel()
    expenses = data["expenses"]
    if category:
        if category not in CATEGORIES:
            raise ValueError(f"неизвестная категория '{category}'")
        total = sum(e["amount"] for e in expenses if e["category"] == category)
        return f"Потрачено на «{category}»: {total:.0f} руб."

    by_cat = {}
    for e in expenses:
        by_cat[e["category"]] = by_cat.get(e["category"], 0) + e["amount"]
    spent = sum(by_cat.values())
    lines = [f"{c}: {s:.0f}" for c, s in sorted(by_cat.items(), key=lambda x: -x[1])]
    result = f"Потрачено {spent:.0f} руб. ({'; '.join(lines) or 'расходов нет'})."
    if data["budget"]:
        left = data["budget"] - spent
        result += (f" Бюджет {data['budget']:.0f}, осталось {left:.0f} "
                   f"({spent / data['budget'] * 100:.0f}% использовано).")
        if left < 0:
            result += " ПЕРЕРАСХОД!"
    else:
        result += " Бюджет не задан."
    return result


TOOLS = [
    {
        "type": "function",
        "function": {
            "name": "list_files",
            "description": "Показывает список файлов в workspace. "
                           "Вызывай, когда нужно узнать, какие файлы доступны.",
            "parameters": {"type": "object", "properties": {}, "required": []},
        },
    },
    {
        "type": "function",
        "function": {
            "name": "read_file",
            "description": "Читает текстовый файл из workspace.",
            "parameters": {
                "type": "object",
                "properties": {
                    "path": {"type": "string",
                             "description": "Путь относительно workspace, например README.md"},
                },
                "required": ["path"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "write_file",
            "description": "Создаёт или полностью перезаписывает текстовый файл в workspace. "
                           "Вызывай, когда пользователь просит создать или изменить файл. "
                           "Перед записью пользователь подтверждает действие.",
            "parameters": {
                "type": "object",
                "properties": {
                    "path": {"type": "string",
                             "description": "Путь относительно workspace, например TODO.md"},
                    "content": {"type": "string",
                                "description": "Полное новое содержимое файла"},
                },
                "required": ["path", "content"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "search_docs",
            "description": "Ищет по документации приложения Travel Planner (workspace/docs: возможности, "
                           "бюджет и расходы, конвертер валют, FAQ) и возвращает самые подходящие "
                           "фрагменты с путём к файлу. Вызывай первым, когда вопрос о том, как "
                           "работает приложение, или когда для задачи нужен факт из документации.",
            "parameters": {
                "type": "object",
                "properties": {
                    "query": {"type": "string",
                              "description": "Ключевые слова, например «бюджет демо-поездки»"},
                    "limit": {"type": "integer", "minimum": 1, "maximum": 5,
                              "description": "Сколько фрагментов вернуть, по умолчанию 3"},
                },
                "required": ["query"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "set_budget",
            "description": "Задаёт общий бюджет поездки в рублях. "
                           "Вызывай, когда пользователь называет бюджет.",
            "parameters": {
                "type": "object",
                "properties": {
                    "amount": {"type": "number", "description": "Бюджет в рублях, больше 0"},
                },
                "required": ["amount"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "add_expense",
            "description": "Добавляет один расход поездки. Вызывай для каждой траты, "
                           "которую называет пользователь (несколько трат — несколько вызовов).",
            "parameters": {
                "type": "object",
                "properties": {
                    "amount": {"type": "number", "description": "Сумма в рублях, больше 0"},
                    "category": {"type": "string", "enum": CATEGORIES,
                                 "description": "Категория расхода"},
                    "note": {"type": "string",
                             "description": "Короткое описание, например «кофе» или «такси в аэропорт»"},
                },
                "required": ["amount", "category"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "budget_status",
            "description": "Показывает, сколько потрачено (всего и по категориям) и сколько "
                           "осталось от бюджета. Вызывай, когда спрашивают про траты, "
                           "остаток или перерасход.",
            "parameters": {
                "type": "object",
                "properties": {
                    "category": {"type": "string", "enum": CATEGORIES,
                                 "description": "Если указана — сумма только по этой категории"},
                },
                "required": [],
            },
        },
    },
]

FUNCTIONS = {
    "list_files": list_files,
    "read_file": read_file,
    "write_file": write_file,
    "search_docs": search_docs,
    "set_budget": set_budget,
    "add_expense": add_expense,
    "budget_status": budget_status,
}


# ---------- выполнение инструмента ----------

def execute_tool(name, raw_args):
    """Разбирает JSON-аргументы, вызывает функцию. Любую ошибку возвращает строкой."""
    try:
        args = json.loads(raw_args or "{}")
        func = FUNCTIONS.get(name)
        if func is None:
            return f"ОШИБКА: неизвестный инструмент '{name}'"
        return str(func(**args))
    except Exception as e:
        return f"ОШИБКА: {e}"


# ---------- оценка размера контекста (лаб. 3, шаг 2) ----------

# Описания инструментов уходят в каждом запросе, их размер постоянный — считаем один раз.
TOOLS_CHARS = len(json.dumps(TOOLS, ensure_ascii=False))


def message_chars(m):
    """Сколько символов текста в одном сообщении: content + имена и аргументы tool_calls."""
    chars = len(m.get("content") or "")
    for call in m.get("tool_calls") or []:
        chars += len(call["function"]["name"]) + len(call["function"]["arguments"])
    return chars


def estimate_tokens(messages, with_tools=True):
    """Грубая оценка входа в токенах: ~4 символа на токен (len(text) // 4)."""
    chars = sum(message_chars(m) for m in messages)
    if with_tools:
        chars += TOOLS_CHARS
    return chars // 4


# ---------- бюджет контекста и усечение (лаб. 3, шаг 3) ----------

def is_memory(m):
    return m["role"] == "system" and (m.get("content") or "").startswith(MEMORY_PREFIX)


def split_turns(messages):
    """Делит историю на «голову» (системный промпт) и ходы.
    Ход = сообщение user + все ответы assistant и tool после него до следующего user.
    Ход удаляется только целиком, поэтому tool_calls и их ответы role=tool не разрываются."""
    head = [messages[0]]
    start = 1
    if len(messages) > 1 and is_memory(messages[1]):
        head.append(messages[1])  # память диалога (шаг 4) — тоже «голова», не удаляется
        start = 2
    turns = []
    for m in messages[start:]:
        if m["role"] == "user" or not turns:
            turns.append([m])
        else:
            turns[-1].append(m)
    return head, turns


def flatten(turns):
    return [m for turn in turns for m in turn]


def shorten_tool_results(turn):
    """Сокращает длинные результаты инструментов в текущем ходе, кроме самых свежих
    (тех, что после последнего ответа ассистента: модель ещё не видела их).
    Сообщения остаются на месте, меняется только текст, поэтому история валидна."""
    last_assistant = max((i for i, m in enumerate(turn) if m["role"] == "assistant"), default=-1)
    shortened = 0
    for m in turn[:last_assistant]:
        content = m.get("content") or ""
        if m["role"] == "tool" and len(content) > TOOL_RESULT_KEEP and SHORTENED_MARK not in content:
            m["content"] = content[:TOOL_RESULT_KEEP] + f"\n{SHORTENED_MARK}, было {len(content)} символов]"
            shortened += 1
    return shortened


def render_for_summary(messages):
    """Превращает сообщения в простой текст для суммаризации (длинные результаты обрезаются)."""
    lines = []
    for m in messages:
        role, content = m["role"], (m.get("content") or "").strip()
        if role == "user":
            lines.append(f"Пользователь: {content}")
        elif role == "assistant":
            for call in m.get("tool_calls") or []:
                lines.append(f"Агент вызвал {call['function']['name']}({call['function']['arguments'][:200]})")
            if content:
                lines.append(f"Агент: {content[:500]}")
        elif role == "tool":
            lines.append(f"Результат инструмента: {content[:300]}")
    return "\n".join(lines)


SUMMARY_PROMPT = (
    "Ты сжимаешь старую часть диалога пользователя с агентом Travel Planner в короткую "
    "заметку-память, чтобы агент мог продолжить работу без полной истории. "
    "Пиши по-русски, только факты из диалога, ничего не выдумывай, до 600 символов, "
    "без вступлений. Формат строго:\n"
    "Пользователя зовут: … Цель: … Ранее сделано: … Важные факты и числа: … Открытые задачи: …\n"
    "Если чего-то нет в диалоге — пиши «неизвестно». "
    "Если дана предыдущая память, объедини её с новым фрагментом."
)


def summarize_messages(old_memory, removed):
    """Шаг 4: отдельный короткий запрос к модели БЕЗ tools. Возвращает текст памяти или None."""
    text = render_for_summary(removed)
    if old_memory:
        text = f"Предыдущая память:\n{old_memory}\n\nНовый фрагмент диалога:\n{text}"
    request = [
        {"role": "system", "content": SUMMARY_PROMPT},
        {"role": "user", "content": text},
    ]
    print(f"  SUMMARY: запрос к модели без tools, вход ≈ {estimate_tokens(request, with_tools=False)} ток.")
    try:
        response = call_model(request, use_tools=False)
    except Exception as e:
        print(f"  SUMMARY: ошибка запроса: {e}")
        return None
    summary = (response.choices[0].message.content or "").strip()
    print(f"  SUMMARY: usage prompt={response.usage.prompt_tokens}, "
          f"completion={response.usage.completion_tokens}")
    if not summary:
        return None
    # На всякий случай: модель могла сама дописать префикс или выйти за лимит.
    summary = summary.removeprefix(MEMORY_PREFIX).strip()
    return summary[:SUMMARY_MAX_CHARS]


def ensure_context_budget(messages):
    """Следит, чтобы оценка входа не превышала CONTEXT_BUDGET.
    1) Системный промпт и текущий ход (последний вопрос + его tool-цикл) сохраняются всегда.
    2) Самые старые ходы удаляются целиком, пока не уложимся в бюджет.
       При STRATEGY=summarize удалённое сжимается в одно сообщение «[память диалога] …»,
       которое кладётся сразу после system (шаг 4).
    3) Если всё ещё много — сокращаются старые результаты инструментов текущего хода.
    Меняет список messages на месте (messages[:] = ...), чтобы изменения увидел вызывающий код."""
    before = estimate_tokens(messages)
    if before <= CONTEXT_BUDGET:
        return

    head, turns = split_turns(messages)
    # При суммаризации старая память заменится новой, а под новую нужен запас.
    summarize = STRATEGY == "summarize"
    base = head[:1] if summarize else head
    target = CONTEXT_BUDGET - SUMMARY_RESERVE if summarize else CONTEXT_BUDGET
    removed = []
    while len(turns) > 1 and estimate_tokens(base + flatten(turns)) > target:
        removed.extend(turns.pop(0))  # самый старый ход — целиком

    if removed and summarize:
        old_memory = head[1]["content"] if len(head) > 1 else ""
        summary = summarize_messages(old_memory, removed)
        if summary:
            head = [head[0], {"role": "system", "content": f"{MEMORY_PREFIX} {summary}"}]
            after = estimate_tokens(head + flatten(turns))
            print(f"  SUMMARY: сжато {len(removed)} старых сообщений; ≈{before} → ≈{after} ток. "
                  f"(бюджет {CONTEXT_BUDGET})")
            print(f"  SUMMARY: {head[1]['content'][:300]}")
        else:
            print("  SUMMARY: не получилось — fallback на TRIM (старая память сохранена)")

    if removed and not summarize:
        after = estimate_tokens(head + flatten(turns))
        print(f"  TRIM: removed {len(removed)} старых сообщений; ≈{before} → ≈{after} ток. "
              f"(бюджет {CONTEXT_BUDGET})")

    if turns and estimate_tokens(head + flatten(turns)) > CONTEXT_BUDGET:
        n = shorten_tool_results(turns[-1])
        if n:
            print(f"  TRIM: сокращено результатов инструментов: {n}; "
                  f"≈{estimate_tokens(head + flatten(turns))} ток.")

    messages[:] = head + flatten(turns)
    final = estimate_tokens(messages)
    if final > CONTEXT_BUDGET:
        # Падать нельзя: отправляем как есть и предупреждаем.
        print(f"  WARN: ≈{final} ток. всё ещё больше бюджета {CONTEXT_BUDGET}, сокращать больше нечего")


# ---------- запрос к модели ----------

def call_model(messages, retries=4, use_tools=True):
    """Запрос к модели с повтором: если сервер вернул ошибку вместо ответа
    или ответил 429 (лимит запросов) / 5xx (перегружен) — ждём и пробуем снова.
    use_tools=False — запрос без инструментов (для суммаризации, шаг 4)."""
    extra = {"tools": TOOLS} if use_tools else {}
    for attempt in range(1, retries + 1):
        try:
            response = client.chat.completions.create(
                model=MODEL,
                messages=messages,
                max_tokens=2000,
                extra_body={"reasoning": {"effort": "low"}},
                **extra,
            )
        except (RateLimitError, InternalServerError) as e:
            wait = 10 * attempt
            print(f"  ! {e.status_code}: сервер занят (попытка {attempt}/{retries}), жду {wait} с")
            time.sleep(wait)
            continue
        if response.choices:
            return response
        error = getattr(response, "error", None) or response.model_dump()
        print(f"  ! сервер вернул ошибку (попытка {attempt}/{retries}): {error}")
        time.sleep(5 * attempt)
    raise RuntimeError("модель не ответила после нескольких попыток")


# ---------- агентный цикл ----------

def run_agent(messages, on_step=None):
    """on_step — функция, которая вызывается после каждого шага (сохранение сессии)."""
    for step in range(1, MAX_STEPS + 1):
        print()
        ensure_context_budget(messages)  # перед КАЖДЫМ запросом к API
        estimate = estimate_tokens(messages)
        print(f"[шаг {step}] CONTEXT: оценка ≈ {estimate} / бюджет {CONTEXT_BUDGET} ток., "
              f"сообщений: {len(messages)}")
        response = call_model(messages)
        msg = response.choices[0].message
        print(f"[шаг {step}] usage: prompt={response.usage.prompt_tokens}, "
              f"completion={response.usage.completion_tokens} "
              f"(оценка/факт = {estimate / max(response.usage.prompt_tokens, 1):.2f})")

        # сохраняем ответ ассистента в историю (вместе с tool_calls)
        messages.append(msg.model_dump(exclude_none=True))

        # нет вызовов инструментов -> это финальный ответ
        if not msg.tool_calls:
            return msg.content

        # выполняем каждый вызов и отдаём результат модели
        for call in msg.tool_calls:
            name = call.function.name
            raw_args = call.function.arguments
            print(f"  -> {name}({raw_args[:200]})")

            result = execute_tool(name, raw_args)
            print(f"  <- {result[:200]}")

            messages.append({
                "role": "tool",
                "tool_call_id": call.id,
                "content": result,
            })

        if on_step:
            on_step()  # шаг агента завершён — сохраняем историю

    return f"Остановлено: достигнут лимит шагов MAX_STEPS={MAX_STEPS}."


# ---------- сессии на диске (лаб. 3, шаг 1) ----------

def session_file(session_id):
    """Путь к файлу сессии. id проверяется, чтобы через него нельзя было выйти из sessions/."""
    if not re.fullmatch(r"[A-Za-z0-9_-]{1,64}", session_id):
        raise ValueError("id сессии: только латиница, цифры, _ и -, до 64 символов")
    return SESSIONS_DIR / f"{session_id}.json"


def repair_history(history):
    """Делает историю валидной для API: у каждого assistant с tool_calls должны быть
    ответы role=tool на все его вызовы, а role=tool не может стоять без своего tool_calls.
    Нужно, если программу закрыли посреди шага агента (Ctrl+C) и в файле остался «хвост»."""
    result = []
    i = 0
    while i < len(history):
        m = history[i]
        if m.get("role") == "tool":
            i += 1  # «осиротевший» ответ инструмента — выбрасываем
            continue
        if m.get("role") == "assistant" and m.get("tool_calls"):
            ids = {c["id"] for c in m["tool_calls"]}
            j = i + 1
            tools = []
            while j < len(history) and history[j].get("role") == "tool":
                tools.append(history[j])
                j += 1
            if ids == {t.get("tool_call_id") for t in tools}:
                result.append(m)
                result.extend(tools)
            # иначе цикл инструментов не завершён — отбрасываем его целиком
            i = j
            continue
        result.append(m)
        i += 1
    return result


def load_session(session_id):
    """Возвращает messages: актуальный системный промпт + история из файла (если он есть)."""
    messages = [{"role": "system", "content": SYSTEM_PROMPT}]
    path = session_file(session_id)
    if path.exists():
        data = json.loads(path.read_text(encoding="utf-8"))
        history = data.get("messages", [])
        # Системный промпт берём из кода, а не из файла — иначе при каждой загрузке
        # он бы дублировался. Старый промпт в файле (если попал туда) пропускаем.
        if history and history[0].get("role") == "system" and history[0].get("content") == SYSTEM_PROMPT:
            history = history[1:]
        messages.extend(repair_history(history))
    return messages


def save_session(session_id, messages):
    """Сохраняет историю без основного системного промпта. Секретов (.env) в messages нет:
    ключ API живёт только в объекте client."""
    SESSIONS_DIR.mkdir(exist_ok=True)
    data = {
        "id": session_id,
        "model": MODEL,
        "updated": datetime.now().isoformat(timespec="seconds"),
        "messages": messages[1:],
    }
    path = session_file(session_id)
    tmp = path.with_suffix(".tmp")
    # Пишем во временный файл и переименовываем: если программа упадёт во время записи,
    # старая сессия не испортится.
    tmp.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
    tmp.replace(path)


# ---------- лог в файл ----------

class Tee:
    """Дублирует всё, что печатается в консоль, ещё и в файл лога (для отчёта)."""

    def __init__(self, path):
        self.file = open(path, "a", encoding="utf-8")
        self.stdout = sys.stdout

    def write(self, text):
        self.stdout.write(text)
        self.file.write(text)
        self.file.flush()

    def flush(self):
        self.stdout.flush()


def logged_input(prompt):
    """input(), но введённый текст тоже попадает в лог."""
    text = input(prompt)
    if isinstance(sys.stdout, Tee):
        sys.stdout.file.write(text + "\n")
    return text


# ---------- консольный чат ----------

def parse_args():
    parser = argparse.ArgumentParser(description="Агент Travel Planner")
    parser.add_argument("--session", default=os.getenv("SESSION_ID"),
                        help="id сессии: продолжить сохранённый диалог (или создать новый)")
    parser.add_argument("--log", help="дописывать вывод в этот файл, например logs/lab3_step1.txt")
    return parser.parse_args()


def main():
    args = parse_args()
    if args.log:
        sys.stdout = Tee(args.log)

    # Без --session создаём новую сессию с id по дате и времени.
    session_id = args.session or datetime.now().strftime("%Y-%m-%d_%H-%M-%S")
    print(f"Модель: {MODEL}; бюджет контекста: {CONTEXT_BUDGET} ток.; стратегия: {STRATEGY}")
    messages = load_session(session_id)
    if len(messages) > 1:
        print(f"Сессия '{session_id}' загружена: {len(messages) - 1} сообщений в истории.")
    else:
        print(f"Новая сессия '{session_id}'. Продолжить потом: python agent.py --session {session_id}")

    while True:
        user_input = logged_input("\nВы: ")
        if user_input.lower() in ("exit", "quit", "выход"):
            break

        messages.append({"role": "user", "content": user_input})
        save_session(session_id, messages)  # вопрос сохранён, даже если модель не ответит
        try:
            answer = run_agent(messages, on_step=lambda: save_session(session_id, messages))
        except Exception as e:
            answer = f"ОШИБКА запроса к модели: {e}"
        save_session(session_id, messages)
        print("\nАгент:", answer)


if __name__ == "__main__":
    main()
