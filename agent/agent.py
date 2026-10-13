import json
import os
import time
from pathlib import Path

from dotenv import load_dotenv
from openai import OpenAI

load_dotenv()

client = OpenAI(
    api_key=os.getenv("OPENAI_API_KEY"),
    base_url=os.getenv("OPENAI_BASE_URL"),
    timeout=120,
)
MODEL = os.getenv("OPENAI_MODEL")

WORKSPACE = (Path(__file__).parent / "workspace").resolve()
MAX_STEPS = 2

# данные проектных инструментов (бюджет и расходы поездки)
TRAVEL_FILE = WORKSPACE / "travel.json"
CATEGORIES = ["еда", "транспорт", "жильё", "билеты", "покупки", "другое"]

SYSTEM_PROMPT = (
    "Ты агент проекта Travel Planner. Отвечай кратко, по-русски. "
    "Ты работаешь только с файлами внутри workspace. "
    "Не придумывай содержимое файлов: чтобы узнать, какие файлы есть, "
    "вызывай list_files, чтобы прочитать файл, вызывай read_file. "
    "Чтобы создать или изменить файл, вызывай write_file — "
    "пользователь должен подтвердить запись. Если запись отклонена, "
    "не пытайся записать снова без новой просьбы пользователя. "
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
    answer = input("  Разрешить? [y/N] ")
    if answer.strip().lower() != "y":
        return "ОТКАЗ: пользователь запретил запись файла."
    full.parent.mkdir(parents=True, exist_ok=True)
    full.write_text(content, encoding="utf-8")
    return f"Файл '{path}' записан ({len(content)} символов)."


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


# ---------- запрос к модели ----------

def call_model(messages, retries=3):
    """Запрос к модели с повтором, если сервер вернул ошибку вместо ответа."""
    for attempt in range(1, retries + 1):
        response = client.chat.completions.create(
            model=MODEL,
            messages=messages,
            tools=TOOLS,
            max_tokens=2000,
            extra_body={"reasoning": {"effort": "low"}},
        )
        if response.choices:
            return response
        error = getattr(response, "error", None) or response.model_dump()
        print(f"  ! сервер вернул ошибку (попытка {attempt}/{retries}): {error}")
        time.sleep(5 * attempt)
    raise RuntimeError("модель не ответила после нескольких попыток")


# ---------- агентный цикл ----------

def run_agent(messages):
    for step in range(1, MAX_STEPS + 1):
        response = call_model(messages)
        msg = response.choices[0].message
        print(f"\n[шаг {step}] usage: prompt={response.usage.prompt_tokens}, "
              f"completion={response.usage.completion_tokens}")

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

    return f"Остановлено: достигнут лимит шагов MAX_STEPS={MAX_STEPS}."


# ---------- консольный чат ----------

def main():
    messages = [{"role": "system", "content": SYSTEM_PROMPT}]

    while True:
        user_input = input("\nВы: ")
        if user_input.lower() in ("exit", "quit", "выход"):
            break

        messages.append({"role": "user", "content": user_input})
        try:
            answer = run_agent(messages)
        except Exception as e:
            answer = f"ОШИБКА запроса к модели: {e}"
        print("\nАгент:", answer)


if __name__ == "__main__":
    main()
