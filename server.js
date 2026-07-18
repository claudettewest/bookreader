const http = require('http');
const fs = require('fs');
const path = require('path');

const port = Number(process.env.PORT) || 3511;
const root = path.join(__dirname, 'src');
const dataRoot = path.join(__dirname, 'data');
const libraryFile = path.join(dataRoot, 'imported-books.json');
const types = {
  '.html': 'text/html; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.json': 'application/json; charset=utf-8'
};

function sendJson(response, status, value) {
  response.writeHead(status, { 'Content-Type': 'application/json; charset=utf-8' });
  response.end(JSON.stringify(value));
}

async function readLibrary() {
  try {
    return JSON.parse(await fs.promises.readFile(libraryFile, 'utf8'));
  } catch (error) {
    if (error.code === 'ENOENT') return [];
    throw error;
  }
}

async function writeLibrary(books) {
  await fs.promises.mkdir(dataRoot, { recursive: true });
  const temporaryFile = `${libraryFile}.tmp`;
  await fs.promises.writeFile(temporaryFile, JSON.stringify(books), 'utf8');
  await fs.promises.rename(temporaryFile, libraryFile);
}

function readJsonBody(request, limit = 100 * 1024 * 1024) {
  return new Promise((resolve, reject) => {
    const chunks = [];
    let size = 0;
    request.on('data', chunk => {
      size += chunk.length;
      if (size > limit) {
        reject(Object.assign(new Error('Book is too large'), { status: 413 }));
        request.destroy();
        return;
      }
      chunks.push(chunk);
    });
    request.on('end', () => {
      try { resolve(JSON.parse(Buffer.concat(chunks).toString('utf8'))); }
      catch { reject(Object.assign(new Error('Invalid JSON'), { status: 400 })); }
    });
    request.on('error', reject);
  });
}

http.createServer(async (request, response) => {
  const rawPath = decodeURIComponent(request.url.split('?')[0]);

  try {
    if (rawPath === '/api/books' && request.method === 'GET') {
      const books = await readLibrary();
      if (new URL(request.url, 'http://localhost').searchParams.get('summary') === '1') {
        sendJson(response, 200, books.map(({chapters, ...book}) => ({...book, serverStored:true})));
      } else {
        sendJson(response, 200, books);
      }
      return;
    }
    if (rawPath === '/api/books' && request.method === 'PUT') {
      const book = await readJsonBody(request);
      if (!book || typeof book.id !== 'string' || !book.id.startsWith('imported-')) {
        sendJson(response, 400, { error: 'Invalid imported book' });
        return;
      }
      const books = await readLibrary();
      const index = books.findIndex(item => item.id === book.id);
      if (index >= 0) books[index] = book;
      else books.push(book);
      await writeLibrary(books);
      sendJson(response, 200, { saved: true });
      return;
    }
    if (rawPath.startsWith('/api/books/') && request.method === 'DELETE') {
      const id = decodeURIComponent(rawPath.slice('/api/books/'.length));
      await writeLibrary((await readLibrary()).filter(book => book.id !== id));
      sendJson(response, 200, { deleted: true });
      return;
    }
    if (rawPath.startsWith('/api/books/') && request.method === 'GET') {
      const id = decodeURIComponent(rawPath.slice('/api/books/'.length));
      const book = (await readLibrary()).find(item => item.id === id);
      if (!book) {
        sendJson(response, 404, { error: 'Book not found' });
        return;
      }
      sendJson(response, 200, book);
      return;
    }
  } catch (error) {
    console.error(error);
    if (!response.headersSent) sendJson(response, error.status || 500, { error: error.message || 'Server error' });
    return;
  }

  const relativePath = rawPath === '/' ? 'index.html' : rawPath.replace(/^\/+/, '');
  const filePath = path.resolve(root, relativePath);

  if (!filePath.startsWith(root + path.sep) && filePath !== path.join(root, 'index.html')) {
    response.writeHead(403);
    response.end('Forbidden');
    return;
  }

  fs.readFile(filePath, (error, data) => {
    if (error) {
      response.writeHead(error.code === 'ENOENT' ? 404 : 500, { 'Content-Type': 'text/plain; charset=utf-8' });
      response.end(error.code === 'ENOENT' ? 'Not found' : 'Server error');
      return;
    }
    response.writeHead(200, { 'Content-Type': types[path.extname(filePath)] || 'application/octet-stream' });
    response.end(data);
  });
}).listen(port, '0.0.0.0', () => {
  console.log(`Book Reader is running at http://localhost:${port}`);
});
