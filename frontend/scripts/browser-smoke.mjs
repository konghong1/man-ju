import { spawn } from "node:child_process";
import { setTimeout as delay } from "node:timers/promises";
import { chromium } from "playwright";

const HOST = process.env.SMOKE_HOST ?? "127.0.0.1";
const PORT = Number(process.env.SMOKE_PORT ?? "5173");
const BASE_URL = process.env.SMOKE_BASE_URL ?? `http://${HOST}:${PORT}`;
const SERVER_TIMEOUT_MS = Number(process.env.SMOKE_SERVER_TIMEOUT_MS ?? "30000");

const routes = [
  { path: "/", heading: "项目列表", text: "路由已就绪：`/`" },
  { path: "/projects/new", heading: "创建项目", text: "路由已就绪：`/projects/new`" },
  { path: "/projects/demo/workspace", heading: "项目工作台", text: "当前项目 ID：demo" },
  { path: "/projects/demo/review", heading: "质量评审", text: "路由已就绪：`/projects/:id/review`" },
  { path: "/projects/demo/export", heading: "导出页面", text: "路由已就绪：`/projects/:id/export`" },
  { path: "/settings/models", heading: "模型设置与指标", text: "支持按厂商与模型选择，并按步骤路由。" },
];

function startDevServer() {
  return spawn(
    process.execPath,
    ["./node_modules/vite/bin/vite.js", "--host", HOST, "--port", String(PORT), "--strictPort"],
    {
      cwd: new URL("..", import.meta.url),
      env: { ...process.env, BROWSER: "none" },
      stdio: ["ignore", "pipe", "pipe"],
    }
  );
}

async function waitForServer(server) {
  const deadline = Date.now() + SERVER_TIMEOUT_MS;
  let lastError;

  while (Date.now() < deadline) {
    if (server.exitCode !== null) {
      throw new Error(`Vite dev server exited early with code ${server.exitCode}.`);
    }

    try {
      const response = await fetch(BASE_URL);
      if (response.ok) {
        return;
      }
      lastError = new Error(`HTTP ${response.status}`);
    } catch (error) {
      lastError = error;
    }

    await delay(250);
  }

  throw new Error(`Timed out waiting for ${BASE_URL}: ${lastError?.message ?? "unknown error"}`);
}

async function stopServer(server) {
  if (server.exitCode !== null) {
    return;
  }

  server.kill("SIGTERM");
  await Promise.race([
    new Promise((resolve) => server.once("exit", resolve)),
    delay(5000).then(() => server.kill("SIGKILL")),
  ]);
}

async function assertVisibleText(page, text) {
  const locator = page.getByText(text, { exact: true });
  await locator.waitFor({ state: "visible", timeout: 5000 });
}

async function main() {
  const server = startDevServer();
  let serverOutput = "";

  server.stdout.on("data", (chunk) => {
    serverOutput += chunk.toString();
  });
  server.stderr.on("data", (chunk) => {
    serverOutput += chunk.toString();
  });

  let browser;
  try {
    await waitForServer(server);
    browser = await chromium.launch({ headless: true });
    const page = await browser.newPage();

    for (const route of routes) {
      await page.goto(`${BASE_URL}${route.path}`, { waitUntil: "networkidle" });
      await page.getByRole("heading", { name: route.heading }).waitFor({ state: "visible" });
      await assertVisibleText(page, route.text);
      console.log(`✓ ${route.path} renders ${route.heading}`);
    }
  } catch (error) {
    if (serverOutput.trim()) {
      console.error("\nVite output:\n" + serverOutput.trim());
    }
    console.error(error);
    process.exitCode = 1;
  } finally {
    if (browser) {
      await browser.close();
    }
    await stopServer(server);
  }
}

await main();
