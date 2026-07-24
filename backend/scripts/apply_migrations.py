import json
import os
from pathlib import Path
from urllib.request import Request, urlopen

project_url = os.environ["SUPABASE_URL"].rstrip("/")
project_ref = project_url.removeprefix("https://").split(".")[0]
token = os.environ["SUPABASE_ACCESS_TOKEN"]
sql_directory = Path(__file__).parents[1] / "sql"
migrations = sorted(sql_directory.glob("[0-9][0-9][0-9]_*.sql"))
if not migrations:
    raise RuntimeError("No numbered migrations found")
sql = "\n".join(path.read_text(encoding="utf-8") for path in migrations)
request = Request(
    f"https://api.supabase.com/v1/projects/{project_ref}/database/query",
    data=json.dumps({"query": sql}).encode(),
    headers={"Authorization": f"Bearer {token}", "Content-Type": "application/json", "User-Agent": "LifeTrack-Migrations/1.0"},
    method="POST",
)
with urlopen(request, timeout=120) as response:
    names = ", ".join(path.name for path in migrations)
    print(f"Migrations applied ({names}): HTTP {response.status}")
