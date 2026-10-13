import os
from dotenv import load_dotenv
from openai import OpenAI

load_dotenv()

client = OpenAI(
    api_key=os.getenv("OPENAI_API_KEY"),
    base_url=os.getenv("OPENAI_BASE_URL"),
    timeout=120,
)
MODEL = os.getenv("OPENAI_MODEL")

SYSTEM_PROMPT = "Ты помощник приложения Travel Planner. Отвечай кратко, по-русски."


def main():
    messages = [{"role": "system", "content": SYSTEM_PROMPT}]

    while True:
        user_input = input("\nВы: ")
        if user_input.lower() in ("exit", "quit", "выход"):
            break

        # messages.append({"role": "user", "content": user_input})

        response = client.chat.completions.create(
            model=MODEL,
            messages=messages,
            max_tokens=2000,
            extra_body={"reasoning": {"effort": "low"}},
        )
        answer = response.choices[0].message.content

       # messages.append({"role": "assistant", "content": answer})

        print("Агент:", answer)
        print(f"[usage: prompt={response.usage.prompt_tokens}, "
              f"completion={response.usage.completion_tokens}]")


if __name__ == "__main__":
    main()