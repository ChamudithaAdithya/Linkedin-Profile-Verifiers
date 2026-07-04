Here is the step-by-step developer guide to safely collecting all four configuration keys for your environment.

---

## 1. Google API Key & Google CX (Custom Search)

These two keys work together to let your Spring Boot backend query Google programmatically without scraping. You get them from two different dashboards.

### Step A: Get `GOOGLE_API_KEY`

1. Go to the [Google Cloud Console](https://console.cloud.google.com/).
2. Create a new project (e.g., `"Simlea-Identity-Engine"`).
3. In the top search bar, search for **"Custom Search API"** and click **Enable**.
4. In the left sidebar, navigate to **APIs & Services** > **Credentials**.
5. Click **+ Create Credentials** at the top, and select **API Key**.
6. Copy the generated string. This is your `GOOGLE_API_KEY`.

### Step B: Get `GOOGLE_CX` (Search Engine ID)

1. Go to the [Google Programmable Search Engine Control Panel](https://programmablesearchengine.google.com/).
2. Click **Add** to create a new search engine instance.
3. Under *What to search*, toggle it to **Search the entire web** (or restrict it to specific domains like `linkedin.com` and `github.com` if you want a highly target-focused engine).
4. Save it, go to the engine's **Overview / Setup** page, and locate the **Search Engine ID**.
5. Copy that alphanumeric identifier. This is your `GOOGLE_CX`.

---

## 2. GITHUB_API_KEY (Personal Access Token)

To increase your local API limits from 60 hits an hour to 5,000 hits an hour, generate a token directly from your GitHub profile.

1. Log into your GitHub account and navigate to [GitHub Token Settings](https://github.com/settings/tokens).
2. Click **Generate new token** > select **Generate new token (classic)**.
3. Give it a descriptive note (e.g., `"Simlea Integration Backend"`).
4. **Important for Privacy:** Under the scopes list, you do **not** need to check any boxes if you are only searching *public* developer profiles. Leave everything unchecked for maximum security.
5. Scroll to the bottom, click **Generate Token**, and copy the string immediately. This is your `GITHUB_API_KEY`. *(Note: GitHub hides this string forever once you refresh the page).*

---

## 3. HUNTER_API_KEY (B2B Email Finder)

This key enables the backend to find and verify corporate email formats based on first names, last names, and domains.

1. Head over to [Hunter.io](https://hunter.io/) and register for a free Developer Account.
2. Once logged into your dashboard, go directly to the [Hunter API Keys Page](https://hunter.io/api-keys).
3. Click **Create an API key** (or copy the default key automatically provisioned upon account setup).
4. Copy the secret key string. This is your `HUNTER_API_KEY`.

---

## 🔐 Best Practice Reminder

Never commit these keys into Git repository files like `application.properties` directly. Instead, reference them using environment variable injection flags in your local project setup:

```properties
# Inside your application.properties file
search.google.api-key=${GOOGLE_API_KEY}
search.google.cx=${GOOGLE_CX}
search.github.api-key=${GITHUB_API_KEY}
search.hunter.api-key=${HUNTER_API_KEY}

```

Then, configure these variables inside your local IDE configuration settings (like IntelliJ IDEA or VS Code environment configurations) or export them directly into your terminal before running the application container!