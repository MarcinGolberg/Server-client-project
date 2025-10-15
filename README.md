# Server–Client Project

A minimal Java server–client example with a plain‑text configuration file and two independent modules: a **server** and a **client**.

## Project layout

```
Server-client-project/
├─ serverPart/
│  ├─ Server.java
│  └─ server_config.txt
└─ clientPart/
   └─ Client.java
```

## Prerequisites

- Java 17+ (JDK). Any newer LTS should work.
- A terminal (PowerShell / cmd on Windows, or any POSIX shell on Linux/macOS).

## Configuration (`server_config.txt`)

The server reads its settings from a simple `key=value` file. Example values used in this project:

```
port=12345
serverName=localhost
bannedPhrases=java,SAD,JNI
```
These lines define the TCP port, the advertised server name/host, and a comma‑separated list of phrases that the server should block in messages.

> Put `server_config.txt` next to `Server.java` (as shown above). If your `Server` program expects a path to the config file as an argument, pass a relative path like `serverPart/server_config.txt`. Otherwise, it will typically try to load `server_config.txt` from its working directory.

## Build & run

You can compile and run each part separately. Commands below assume you’re in the repository root: `Server-client-project/`.

### Option A — Quick compile in-place

**Compile:**

```bash
# Server
javac serverPart/Server.java

# Client
javac clientPart/Client.java
```

**Run:**

Open **two terminals**.

Terminal 1 — start the server:
```bash
# If Server loads config implicitly from the working directory:
cd serverPart
java Server

# If Server expects a config path argument:
# java Server server_config.txt
```

Terminal 2 — start the client:
```bash
cd clientPart
java Client
```

### Option B — Compile to an `out/` directory

```bash
mkdir -p out/server out/client

javac -d out/server serverPart/Server.java
javac -d out/client clientPart/Client.java

# Run (from project root)
java -cp out/server Server serverPart/server_config.txt   # if your Server expects a config path
java -cp out/client Client
```

> If your classes declare a `package`, mirror that package in your folder structure and include it in the `java -cp` commands. If there’s **no** package declaration in the files, the commands above will work as is.

## Typical workflow

1. Adjust `serverPart/server_config.txt` (port, name/host, banned phrases).
2. Start the server. Make sure it prints a “listening on port …” style message (or no errors).
3. Start the client and connect to the server (many simple demos auto‑connect to `localhost:port`).
4. Exchange messages. Any message containing a banned phrase should be rejected by the server.

## Troubleshooting

- **`Address already in use` / cannot bind port**
  Another service uses the same port. Change `port` in `server_config.txt` and restart the server.

- **Client can’t connect**
  - Verify the `port` in the config matches what the client uses.
  - If running on different machines, replace `serverName=localhost` with the server’s LAN IP or hostname and make sure firewalls allow the chosen port.

- **Banned phrases don’t seem to work**
  Ensure `bannedPhrases` is a comma‑separated list with no spaces (e.g., `a,b,c`). Restart the server after changes.

## Tips

- Keep the server and client in separate terminals so you can see logs from both.
- When changing `server_config.txt`, restart the server so it reloads the file.
- For multi‑machine tests, confirm you can ping the server host from the client machine first.
