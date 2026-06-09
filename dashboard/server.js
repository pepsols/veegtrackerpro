const http = require("http");
const fs = require("fs");
const path = require("path");

const host = "127.0.0.1";
const port = Number(process.env.PORT || 4173);
const root = __dirname;

const mimeTypes = {
  ".html": "text/html; charset=utf-8",
  ".css": "text/css; charset=utf-8",
  ".js": "application/javascript; charset=utf-8",
  ".json": "application/json; charset=utf-8",
  ".png": "image/png",
  ".jpg": "image/jpeg",
  ".jpeg": "image/jpeg",
  ".svg": "image/svg+xml",
  ".ico": "image/x-icon"
};

function resolveFile(requestUrl) {
  const cleanUrl = requestUrl.split("?")[0];
  const relativePath = cleanUrl === "/" ? "/login.html" : cleanUrl;
  const normalizedPath = path.normalize(relativePath).replace(/^(\.\.[/\\])+/, "");
  return path.join(root, normalizedPath);
}

const server = http.createServer((req, res) => {
  const filePath = resolveFile(req.url || "/");

  fs.stat(filePath, (statError, stats) => {
    if (statError || !stats.isFile()) {
      res.writeHead(404, { "Content-Type": "text/plain; charset=utf-8" });
      res.end("Niet gevonden");
      return;
    }

    const extension = path.extname(filePath).toLowerCase();
    const contentType = mimeTypes[extension] || "application/octet-stream";

    fs.readFile(filePath, (readError, content) => {
      if (readError) {
        res.writeHead(500, { "Content-Type": "text/plain; charset=utf-8" });
        res.end("Serverfout");
        return;
      }

      res.writeHead(200, { "Content-Type": contentType });
      res.end(content);
    });
  });
});

server.listen(port, host, () => {
  console.log(`Veegtracker dashboard draait op http://${host}:${port}`);
});
