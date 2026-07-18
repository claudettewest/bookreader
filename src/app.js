const resetBooks = JSON.parse(localStorage.getItem('bookReaderReset') || '[]');
let books = [
  { id:'garden', title:"The Cartographer's Garden", author:'Elena Vale', cover:'cover-garden', progress:34, kicker:'A NOVEL' },
  { id:'sea', title:'Salt Between Stars', author:'Mara Lin', cover:'cover-sea', progress:72, kicker:'A VOYAGE' },
  { id:'hours', title:'The Quiet Hours', author:'Theo Maren', cover:'cover-hours', progress:0, kicker:'ESSAYS' },
  { id:'wildwood', title:'Wildwood Almanac', author:'Iris North', cover:'cover-wildwood', progress:18, kicker:'FIELD NOTES' },
  { id:'letters', title:'Letters to the Moon', author:'Ana Sol', cover:'cover-letters', progress:100, kicker:'POEMS' }
].map(book => resetBooks.includes(book.id) ? {...book, progress:0} : book)
  .filter(book => !JSON.parse(localStorage.getItem('bookReaderDeleted') || '[]').includes(book.id));

const chapterNames = ['The Brass Compass','A Street Without a Name','The Greenhouse Door','The Map That Remembered Rain','Ink in the Well','The Orchard at Dusk','North of Yesterday','A Country of Small Things','The River in the Margin','What the Atlas Hid','The Last Fold','Home, Drawn by Hand'];
const chapterDecks = [
  'Every journey begins twice: once in the mind, and once beneath the feet.',
  'A city keeps its secrets in the names it refuses to use.',
  'Behind every locked door is a weather of its own.',
  'Some places are easier to find after they have disappeared.',
  'The truest lines are the ones the hand draws without knowing.',
  'At evening, even familiar trees begin to speak in riddles.',
  'Memory has a direction, though no compass can hold it.',
  'What we overlook is often what makes a place whole.',
  'Every margin is another kind of country.',
  'The best secrets wait in plain sight, folded small.',
  'To finish a map, one must first accept what it cannot contain.',
  'Home is not a point. It is the line we keep returning to.'
];
const baseParagraphs = [
  'Mara found the map on a morning when the rain had forgotten how to fall. It hung in the air instead, a silver hush above the tiled roofs, while gutters waited with their mouths open and the city carried on beneath a sky full of unfinished weather.',
  'The paper was tucked behind a row of atlases in her grandfather’s shop. It was warmer than it should have been. When she spread it across the long oak table, a single green line appeared at its centre, trembling like a stem in wind.',
  'There were rules for maps in Bellweather. Rivers wore blue. Roads were precise. Gardens were marked with a small square of green. But this line ignored every convention she had been taught. It wandered through walls, crossed the blank space beyond the old quarter, and ended in a symbol she did not recognise: a tiny open door.',
  'Outside, the bells marked the ninth hour. Her grandfather would return soon with ink and fresh paper. Mara leaned closer. Along the bottom edge, letters began to surface one by one, written in the narrow hand she knew from childhood birthday cards and labels on jars.',
  'Follow only what remembers you, the map said.',
  'She touched the words. Somewhere deep in the shop, among rolled charts and brass instruments, a drop of rain struck the floor. Then another. The green line shifted beneath her finger, turning gently east.',
  'Mara folded the map along its oldest crease and slipped it into her coat. For the first time in seven years, the painted compass above the door swung away from north. It pointed toward the narrow lane behind the shop, where ivy covered a wall that, until that morning, had never contained a gate.'
];

const state = {
  view: 'library',
  book: 'garden',
  chapter: Number(localStorage.getItem('bookReaderChapter')) || 3,
  fontSize: Number(localStorage.getItem('bookReaderFontSize')) || 18,
  width: Number(localStorage.getItem('bookReaderWidth')) || 680,
  tone: localStorage.getItem('bookReaderTone') || 'paper',
  bookmarks: JSON.parse(localStorage.getItem('bookReaderBookmarks') || '[]'),
  menuBookId: null
};

const speechState = {
  paragraph: 0,
  session: 0,
  speaking: false,
  paused: false,
  volume: Number(localStorage.getItem('bookReaderSpeechVolume') ?? 0.8),
  voices: []
};

const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => [...document.querySelectorAll(selector)];
const escapeHtml = (value = '') => String(value).replace(/[&<>'"]/g, character => ({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[character]));
const libraryView = $('#library-view');
const readerView = $('#reader-view');
const savedView = $('#saved-view');
const toast = $('#toast');

function showToast(message) {
  toast.textContent = message;
  toast.classList.add('show');
  clearTimeout(showToast.timer);
  showToast.timer = setTimeout(() => toast.classList.remove('show'), 2200);
}

function updateSpeechControls(status = speechState.paused ? 'Paused' : speechState.speaking ? 'Reading' : 'Ready') {
  $('#speech-status').textContent = status;
  $('#speech-play').classList.toggle('playing', speechState.speaking && !speechState.paused);
  $('#speech-play').setAttribute('aria-label', speechState.speaking && !speechState.paused ? 'Pause' : 'Play');
  $('#tts-button').classList.toggle('active', speechState.speaking || !$('#speech-panel').hidden);
}

function populateDeviceVoices() {
  if (!('speechSynthesis' in window)) return;
  speechState.voices = window.speechSynthesis.getVoices();
  const select = $('#speech-voice');
  const savedVoice = localStorage.getItem('bookReaderSpeechVoice') || '';
  select.replaceChildren(new Option('Device default', ''));
  speechState.voices.forEach(voice => select.add(new Option(`${voice.name} · ${voice.lang}`, voice.voiceURI)));
  if ([...select.options].some(option => option.value === savedVoice)) select.value = savedVoice;
}

function clearSpeechHighlight() {
  $$('#chapter-copy p.speaking').forEach(paragraph => paragraph.classList.remove('speaking'));
}

function finishSpeech() {
  speechState.speaking = false;
  speechState.paused = false;
  speechState.paragraph = 0;
  clearSpeechHighlight();
  updateSpeechControls('Complete');
}

function speakParagraph(session) {
  if (session !== speechState.session || !speechState.speaking) return;
  const paragraphs = $$('#chapter-copy p');
  if (speechState.paragraph >= paragraphs.length) { finishSpeech(); return; }
  const paragraph = paragraphs[speechState.paragraph];
  clearSpeechHighlight();
  paragraph.classList.add('speaking');
  paragraph.scrollIntoView({behavior:'smooth', block:'center'});
  const utterance = new SpeechSynthesisUtterance(paragraph.textContent);
  utterance.volume = speechState.volume;
  const selectedVoice = speechState.voices.find(voice => voice.voiceURI === $('#speech-voice').value);
  if (selectedVoice) utterance.voice = selectedVoice;
  utterance.onstart = () => { if (session === speechState.session) updateSpeechControls('Reading'); };
  utterance.onend = () => {
    if (session !== speechState.session) return;
    speechState.paragraph += 1;
    speakParagraph(session);
  };
  utterance.onerror = event => {
    if (session !== speechState.session || event.error === 'canceled' || event.error === 'interrupted') return;
    stopSpeech();
    showToast('Your device could not read this passage');
  };
  window.speechSynthesis.speak(utterance);
}

function startSpeech() {
  if (!('speechSynthesis' in window) || typeof SpeechSynthesisUtterance === 'undefined') {
    showToast('Text-to-speech is not available on this device');
    return;
  }
  const paragraphs = $$('#chapter-copy p');
  if (!paragraphs.length) { showToast('There is no readable text in this chapter'); return; }
  speechState.session += 1;
  speechState.paragraph = 0;
  speechState.speaking = true;
  speechState.paused = false;
  updateSpeechControls('Starting…');
  speakParagraph(speechState.session);
}

function toggleSpeech() {
  if (!speechState.speaking) { startSpeech(); return; }
  if (speechState.paused) {
    window.speechSynthesis.resume();
    speechState.paused = false;
    updateSpeechControls('Reading');
  } else {
    window.speechSynthesis.pause();
    speechState.paused = true;
    updateSpeechControls('Paused');
  }
}

function stopSpeech(status = 'Ready') {
  speechState.session += 1;
  if ('speechSynthesis' in window) window.speechSynthesis.cancel();
  speechState.speaking = false;
  speechState.paused = false;
  speechState.paragraph = 0;
  clearSpeechHighlight();
  updateSpeechControls(status);
}

function restartSpeechParagraph() {
  if (!speechState.speaking) return;
  const paragraph = speechState.paragraph;
  speechState.session += 1;
  window.speechSynthesis.cancel();
  speechState.paragraph = paragraph;
  speechState.paused = false;
  const session = speechState.session;
  setTimeout(() => speakParagraph(session), 60);
}

function openBookDatabase() {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open('book-reader-library', 1);
    request.onupgradeneeded = () => request.result.createObjectStore('books', {keyPath:'id'});
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

async function saveImportedBook(book) {
  const response = await fetch('/api/books', {
    method: 'PUT',
    headers: {'Content-Type':'application/json'},
    body: JSON.stringify(book)
  });
  if (!response.ok) throw new Error((await response.json().catch(() => ({}))).error || 'Book could not be saved');

  // Keep a browser copy so previously imported libraries continue to work offline.
  const database = await openBookDatabase();
  await new Promise((resolve, reject) => {
    const transaction = database.transaction('books', 'readwrite');
    transaction.objectStore('books').put(book);
    transaction.oncomplete = resolve;
    transaction.onerror = () => reject(transaction.error);
  });
  database.close();
}

async function deleteImportedBook(id) {
  fetch(`/api/books/${encodeURIComponent(id)}`, {method:'DELETE'}).catch(error => {
    console.warn('Imported book could not be deleted from the server', error);
  });
  const database = await openBookDatabase();
  const transaction = database.transaction('books', 'readwrite');
  transaction.objectStore('books').delete(id);
  transaction.oncomplete = () => database.close();
}

async function restoreImportedBooks() {
  const loadingStarted = performance.now();
  let imported = [];
  let serverAvailable = false;
  try {
    const response = await fetch('/api/books?summary=1');
    if (!response.ok) throw new Error('Saved books could not be loaded');
    imported = await response.json();
    serverAvailable = true;
  } catch (error) {
    console.warn('Server library could not be restored; trying browser storage', error);
  }

  try {
    const database = await openBookDatabase();
    const browserBooks = await new Promise((resolve, reject) => {
      const request = database.transaction('books').objectStore('books').getAll();
      request.onsuccess = () => resolve(request.result);
      request.onerror = () => reject(request.error);
    });
    database.close();
    const importedIds = new Set(imported.map(book => book.id));
    const browserOnly = browserBooks.filter(book => !importedIds.has(book.id));
    imported.push(...browserOnly);
    if (serverAvailable) {
      browserOnly.forEach(book => fetch('/api/books', {
        method:'PUT', headers:{'Content-Type':'application/json'}, body:JSON.stringify(book)
      }).catch(error => console.warn('Existing book could not be migrated to server storage', error)));
    }
  } catch (error) {
    console.warn('Browser library could not be restored', error);
  }

  try {
    const known = new Set(books.map(book => book.id));
    books.push(...imported.filter(book => !known.has(book.id)));
    renderBooks($('#library-search').value);
  } catch (error) {
    console.warn('Imported library could not be rendered', error);
  }

  const loader = $('#library-loader');
  const minimumDisplayTime = 650;
  setTimeout(() => {
    loader.hidden = true;
    libraryView.setAttribute('aria-busy', 'false');
  }, Math.max(0, minimumDisplayTime - (performance.now() - loadingStarted)));
}

function normalizeArchivePath(path) {
  const parts = [];
  decodeURIComponent(path).replace(/\\/g, '/').split('/').forEach(part => {
    if (!part || part === '.') return;
    if (part === '..') parts.pop();
    else parts.push(part);
  });
  return parts.join('/');
}

function createZipReader(buffer) {
  const view = new DataView(buffer);
  const bytes = new Uint8Array(buffer);
  const decoder = new TextDecoder();
  let end = bytes.length - 22;
  const minimum = Math.max(0, bytes.length - 65557);
  while (end >= minimum && view.getUint32(end, true) !== 0x06054b50) end--;
  if (end < minimum) throw new Error('This EPUB is not a valid ZIP archive');
  const entries = new Map();
  let cursor = view.getUint32(end + 16, true);
  const count = view.getUint16(end + 10, true);
  for (let index = 0; index < count; index++) {
    if (view.getUint32(cursor, true) !== 0x02014b50) break;
    const method = view.getUint16(cursor + 10, true);
    const size = view.getUint32(cursor + 20, true);
    const nameLength = view.getUint16(cursor + 28, true);
    const extraLength = view.getUint16(cursor + 30, true);
    const commentLength = view.getUint16(cursor + 32, true);
    const localOffset = view.getUint32(cursor + 42, true);
    const name = normalizeArchivePath(decoder.decode(bytes.slice(cursor + 46, cursor + 46 + nameLength)));
    entries.set(name, {method, size, localOffset});
    cursor += 46 + nameLength + extraLength + commentLength;
  }
  return {
    has: path => entries.has(normalizeArchivePath(path)),
    async read(path) {
      const entry = entries.get(normalizeArchivePath(path));
      if (!entry) throw new Error(`Missing EPUB resource: ${path}`);
      const nameLength = view.getUint16(entry.localOffset + 26, true);
      const extraLength = view.getUint16(entry.localOffset + 28, true);
      const start = entry.localOffset + 30 + nameLength + extraLength;
      const compressed = bytes.slice(start, start + entry.size);
      if (entry.method === 0) return compressed;
      if (entry.method !== 8 || typeof DecompressionStream === 'undefined') throw new Error('This EPUB uses unsupported compression');
      const stream = new Blob([compressed]).stream().pipeThrough(new DecompressionStream('deflate-raw'));
      return new Uint8Array(await new Response(stream).arrayBuffer());
    }
  };
}

function xmlElements(document, localName) {
  return [...document.getElementsByTagNameNS('*', localName)];
}

function resourceMimeType(path, declaredType = '') {
  if (declaredType.startsWith('image/')) return declaredType;
  const extension = path.split('.').pop().toLowerCase();
  return ({jpg:'image/jpeg',jpeg:'image/jpeg',png:'image/png',gif:'image/gif',webp:'image/webp',svg:'image/svg+xml',avif:'image/avif'}[extension] || 'application/octet-stream');
}

function bytesToDataUrl(bytes, mimeType) {
  let binary = '';
  for (let offset = 0; offset < bytes.length; offset += 0x8000) {
    binary += String.fromCharCode(...bytes.subarray(offset, offset + 0x8000));
  }
  return `data:${mimeType};base64,${btoa(binary)}`;
}

async function parseEpub(file) {
  const archive = createZipReader(await file.arrayBuffer());
  const decoder = new TextDecoder();
  const parser = new DOMParser();
  const container = parser.parseFromString(decoder.decode(await archive.read('META-INF/container.xml')), 'application/xml');
  const packagePath = xmlElements(container, 'rootfile')[0]?.getAttribute('full-path');
  if (!packagePath) throw new Error('This EPUB has no readable package document');
  const packageDocument = parser.parseFromString(decoder.decode(await archive.read(packagePath)), 'application/xml');
  const title = xmlElements(packageDocument, 'title')[0]?.textContent.trim() || file.name.replace(/\.epub$/i, '');
  const author = xmlElements(packageDocument, 'creator')[0]?.textContent.trim() || 'Unknown author';
  const basePath = packagePath.includes('/') ? packagePath.slice(0, packagePath.lastIndexOf('/') + 1) : '';
  const manifestItems = xmlElements(packageDocument, 'item').map(item => ({
    id:item.getAttribute('id'), href:item.getAttribute('href'), type:item.getAttribute('media-type') || '', properties:item.getAttribute('properties') || ''
  }));
  const manifest = new Map(manifestItems.map(item => [item.id, item.href]));
  const manifestByPath = new Map(manifestItems.map(item => [normalizeArchivePath(basePath + item.href.split('#')[0]), item]));
  const spine = xmlElements(packageDocument, 'itemref').map(item => item.getAttribute('idref'));
  const imageCache = new Map();
  async function readImage(path) {
    const normalizedPath = normalizeArchivePath(path);
    if (!archive.has(normalizedPath)) return null;
    if (!imageCache.has(normalizedPath)) {
      const mimeType = resourceMimeType(normalizedPath, manifestByPath.get(normalizedPath)?.type || '');
      if (!mimeType.startsWith('image/')) return null;
      imageCache.set(normalizedPath, bytesToDataUrl(await archive.read(normalizedPath), mimeType));
    }
    return imageCache.get(normalizedPath);
  }
  const legacyCoverId = xmlElements(packageDocument, 'meta').find(meta => meta.getAttribute('name')?.toLowerCase() === 'cover')?.getAttribute('content');
  const coverItem = manifestItems.find(item => item.properties.split(/\s+/).includes('cover-image'))
    || manifestItems.find(item => item.id === legacyCoverId)
    || manifestItems.find(item => item.type.startsWith('image/') && /(^|[\/_-])cover([._-]|$)/i.test(item.href));
  const coverImage = coverItem ? await readImage(basePath + coverItem.href.split('#')[0]) : null;
  const chapters = [];
  for (const id of spine) {
    const href = manifest.get(id);
    if (!href) continue;
    const resourcePath = normalizeArchivePath(basePath + href.split('#')[0]);
    if (!archive.has(resourcePath)) continue;
    const source = decoder.decode(await archive.read(resourcePath));
    let document = parser.parseFromString(source, 'application/xhtml+xml');
    if (document.querySelector('parsererror')) document = parser.parseFromString(source, 'text/html');
    document.querySelectorAll('script,style').forEach(element => element.remove());
    const heading = document.querySelector('h1,h2,h3,title')?.textContent.trim();
    const resourceBase = resourcePath.includes('/') ? resourcePath.slice(0, resourcePath.lastIndexOf('/') + 1) : '';
    const blocks = [];
    for (const element of document.querySelectorAll('p,blockquote,li,img,svg image')) {
      if (element.matches('p,blockquote,li')) {
        const text = element.textContent.replace(/\s+/g, ' ').trim();
        if (text) blocks.push({type:'text', text});
        continue;
      }
      const source = element.getAttribute('src') || element.getAttribute('href') || element.getAttribute('xlink:href');
      if (!source || /^(data:|https?:|blob:)/i.test(source)) continue;
      const image = await readImage(resourceBase + source.split('#')[0].split('?')[0]);
      if (image) blocks.push({type:'image', src:image, alt:element.getAttribute('alt') || ''});
    }
    let paragraphs = blocks.filter(block => block.type === 'text').map(block => block.text);
    if (!paragraphs.length) {
      paragraphs = (document.body?.textContent || '').split(/\n\s*\n/).map(text => text.replace(/\s+/g, ' ').trim()).filter(Boolean);
      blocks.unshift(...paragraphs.map(text => ({type:'text', text})));
    }
    if (paragraphs.join('').length < 20 && !blocks.some(block => block.type === 'image')) continue;
    chapters.push({title: heading || `Section ${chapters.length + 1}`, paragraphs, blocks});
  }
  if (!chapters.length) throw new Error('No readable chapters were found in this EPUB');
  return {id:`imported-${Date.now()}`, title, author, cover:'cover-imported', coverImage, progress:0, kicker:'EPUB', chapters};
}

async function importBook(file) {
  showToast('Importing your book…');
  let book;
  if (/\.epub$/i.test(file.name) || file.type === 'application/epub+zip') {
    book = await parseEpub(file);
  } else {
    const text = await file.text();
    const paragraphs = text.split(/\n\s*\n/).map(value => value.replace(/\s+/g, ' ').trim()).filter(Boolean);
    if (!paragraphs.length) throw new Error('This text file is empty');
    book = {id:`imported-${Date.now()}`, title:file.name.replace(/\.txt$/i,'').replace(/[-_]/g,' '), author:'Imported text', cover:'cover-imported', progress:0, kicker:'MY BOOK', chapters:[{title:'Begin Here', paragraphs}]};
  }
  await saveImportedBook(book);
  books.unshift(book);
  renderBooks();
  showToast(`“${book.title}” added to your library`);
}

function renderBooks(query = '') {
  const visible = books.filter(book => `${book.title} ${book.author}`.toLowerCase().includes(query.toLowerCase()));
  $('#book-grid').innerHTML = visible.map(book => `
    <article class="book-card" data-open-book="${escapeHtml(book.id)}" tabindex="0" role="button" aria-label="Read ${escapeHtml(book.title)}">
      <div class="book-cover ${book.cover} ${book.coverImage ? 'has-cover-image' : ''}">${book.coverImage ? `<img class="imported-cover-image" src="${escapeHtml(book.coverImage)}" alt="Cover of ${escapeHtml(book.title)}">` : ''}<button class="cover-menu-button" data-book-menu="${escapeHtml(book.id)}" aria-label="Book options for ${escapeHtml(book.title)}" title="Book options">•••</button>${book.coverImage ? '' : `<span class="mini-kicker">${escapeHtml(book.kicker)}</span><strong>${escapeHtml(book.title.toUpperCase()).replace(' ', '<br>')}</strong><small>${escapeHtml(book.author.toUpperCase())}</small>`}</div>
      <h3>${escapeHtml(book.title)}</h3><p>${escapeHtml(book.author)}</p>
      ${book.progress ? `<div class="card-progress"><div class="progress-track"><span style="width:${book.progress}%"></span></div><span>${book.progress}%</span></div>` : ''}
    </article>`).join('');
  $('#empty-state').hidden = visible.length > 0;
  bindBookCards();
  bindBookMenus();
}

function bindBookCards() {
  $$('[data-open-book]').forEach(element => {
    element.onclick = event => { event.stopPropagation(); openReader(element.dataset.openBook); };
    element.onkeydown = event => { if (event.key === 'Enter') openReader(element.dataset.openBook); };
  });
}

function bindBookMenus() {
  $$('[data-book-menu]').forEach(button => {
    button.onclick = event => {
      event.preventDefault();
      event.stopPropagation();
      openBookMenu(button, button.dataset.bookMenu);
    };
  });
}

function openBookMenu(button, bookId) {
  const menu = $('#book-menu');
  const wasOpen = !menu.hidden && state.menuBookId === bookId;
  closeBookMenu();
  if (wasOpen) return;
  state.menuBookId = bookId;
  menu.hidden = false;
  button.setAttribute('aria-expanded', 'true');
  const rect = button.getBoundingClientRect();
  const menuWidth = 184;
  const left = Math.min(window.innerWidth - menuWidth - 10, Math.max(10, rect.right - menuWidth));
  const top = Math.min(window.innerHeight - 105, rect.bottom + 7);
  menu.style.left = `${left}px`;
  menu.style.top = `${top}px`;
}

function closeBookMenu() {
  $('#book-menu').hidden = true;
  $$('[data-book-menu]').forEach(button => button.removeAttribute('aria-expanded'));
  state.menuBookId = null;
}

function resetBookmarkForBook() {
  const book = books.find(item => item.id === state.menuBookId);
  if (!book) return;
  book.progress = 0;
  const reset = JSON.parse(localStorage.getItem('bookReaderReset') || '[]');
  if (!reset.includes(book.id)) reset.push(book.id);
  localStorage.setItem('bookReaderReset', JSON.stringify(reset));
  state.bookmarks = state.bookmarks.filter(mark => mark.book !== book.id);
  if (book.id === 'garden') {
    state.chapter = 0;
    localStorage.setItem('bookReaderChapter', '0');
    const featuredProgress = $('#featured-book .progress-track span');
    if (featuredProgress) featuredProgress.style.width = '0%';
    const featuredPercent = $('#featured-book .progress-row strong');
    if (featuredPercent) featuredPercent.textContent = '0%';
  }
  localStorage.setItem('bookReaderBookmarks', JSON.stringify(state.bookmarks));
  const title = book.title;
  closeBookMenu();
  renderBooks($('#library-search').value);
  showToast(`Bookmark reset for “${title}”`);
}

function deleteBook() {
  const book = books.find(item => item.id === state.menuBookId);
  if (!book) return;
  const deleted = JSON.parse(localStorage.getItem('bookReaderDeleted') || '[]');
  if (!book.id.startsWith('imported-') && !deleted.includes(book.id)) deleted.push(book.id);
  localStorage.setItem('bookReaderDeleted', JSON.stringify(deleted));
  books = books.filter(item => item.id !== book.id);
  if (book.id.startsWith('imported-')) deleteImportedBook(book.id);
  state.bookmarks = state.bookmarks.filter(mark => mark.book !== book.id);
  localStorage.setItem('bookReaderBookmarks', JSON.stringify(state.bookmarks));
  if (book.id === 'garden') $('.continue-section').hidden = true;
  closeBookMenu();
  renderBooks($('#library-search').value);
  showToast(`“${book.title}” deleted`);
}

function getChapterParagraphs(index) {
  const rotations = index % baseParagraphs.length;
  return [...baseParagraphs.slice(rotations), ...baseParagraphs.slice(0, rotations)];
}

function renderChapter() {
  const book = books.find(item => item.id === state.book) || books[0];
  if (!book) { showLibrary(); return; }
  const isGarden = book.id === 'garden';
  const chapters = book.chapters || null;
  const totalChapters = chapters?.length || 12;
  state.chapter = Math.max(0, Math.min(totalChapters - 1, state.chapter));
  const importedChapter = chapters?.[state.chapter];
  const title = importedChapter?.title || (isGarden ? chapterNames[state.chapter] : state.chapter === 0 ? 'Begin Here' : `Part ${state.chapter + 1}`);
  const paragraphs = importedChapter?.paragraphs || getChapterParagraphs(state.chapter);
  $('#reader-title').textContent = book.title;
  $('#reader-author').textContent = book.author;
  $('#chapter-number').textContent = isGarden ? `CHAPTER ${numberWord(state.chapter + 1)}` : `SECTION ${state.chapter + 1}`;
  $('#chapter-title').textContent = title;
  $('#chapter-deck').textContent = importedChapter?.deck || (isGarden ? chapterDecks[state.chapter] : 'A quiet place for the next page to begin.');
  $('#chapter-copy').innerHTML = importedChapter?.blocks?.length
    ? importedChapter.blocks.map(block => block.type === 'image'
      ? `<figure class="epub-image"><img src="${escapeHtml(block.src)}" alt="${escapeHtml(block.alt)}"></figure>`
      : `<p>${escapeHtml(block.text)}</p>`).join('')
    : paragraphs.map(p => `<p>${escapeHtml(p)}</p>`).join('');
  $('#page-number').textContent = state.chapter + 1;
  $('#reader-progress-bar').style.width = `${((state.chapter + 1) / totalChapters) * 100}%`;
  $('#prev-chapter').disabled = state.chapter === 0;
  $('#next-chapter').disabled = state.chapter === totalChapters - 1;
  const contents = chapters?.map(chapter => chapter.title) || chapterNames;
  $('#chapter-list').innerHTML = contents.map((name, index) => `<li class="${index === state.chapter ? 'active' : ''}"><button data-chapter="${index}">${String(index + 1).padStart(2,'0')} &nbsp; ${escapeHtml(name)}</button></li>`).join('');
  $$('[data-chapter]').forEach(button => button.onclick = () => changeChapter(Number(button.dataset.chapter)));
  $('#bookmark-button').classList.toggle('active', state.bookmarks.some(mark => mark.book === state.book && mark.chapter === state.chapter));
  localStorage.setItem('bookReaderChapter', state.chapter);
}

function numberWord(number) {
  return ['ONE','TWO','THREE','FOUR','FIVE','SIX','SEVEN','EIGHT','NINE','TEN','ELEVEN','TWELVE'][number - 1];
}

async function loadImportedBook(book) {
  try {
    const response = await fetch(`/api/books/${encodeURIComponent(book.id)}`);
    if (!response.ok) throw new Error('Saved book could not be loaded');
    Object.assign(book, await response.json(), {serverStored:true});
    return true;
  } catch (serverError) {
    try {
      const database = await openBookDatabase();
      const browserBook = await new Promise((resolve, reject) => {
        const request = database.transaction('books').objectStore('books').get(book.id);
        request.onsuccess = () => resolve(request.result);
        request.onerror = () => reject(request.error);
      });
      database.close();
      if (!browserBook) throw serverError;
      Object.assign(book, browserBook);
      return true;
    } catch (error) {
      console.error('Imported book could not be opened', error);
      showToast('This saved book could not be loaded');
      return false;
    }
  }
}

async function openReader(bookId) {
  stopSpeech();
  $('#speech-panel').hidden = true;
  const selectedBook = books.find(book => book.id === bookId);
  if (selectedBook?.serverStored && !selectedBook.chapters) {
    showToast(`Opening “${selectedBook.title}”…`);
    if (!await loadImportedBook(selectedBook)) return;
  }
  state.book = bookId;
  if (bookId !== 'garden') state.chapter = 0;
  state.view = 'reader';
  libraryView.hidden = true;
  savedView.hidden = true;
  readerView.hidden = false;
  $('.topbar').hidden = true;
  renderChapter();
  applyReaderSettings();
  window.scrollTo(0, 0);
}

function showLibrary() {
  stopSpeech();
  $('#speech-panel').hidden = true;
  state.view = 'library';
  libraryView.hidden = false;
  savedView.hidden = true;
  readerView.hidden = true;
  $('.topbar').hidden = false;
  $$('.nav-item').forEach(item => item.classList.toggle('active', item.dataset.view === 'library'));
  renderBooks($('#library-search').value);
  window.scrollTo(0, 0);
}

function showSaved(type) {
  stopSpeech();
  $('#speech-panel').hidden = true;
  state.view = type;
  libraryView.hidden = true;
  readerView.hidden = true;
  savedView.hidden = false;
  $('.topbar').hidden = false;
  $$('.nav-item').forEach(item => item.classList.toggle('active', item.dataset.view === type));
  $('#saved-title').textContent = type === 'bookmarks' ? 'Bookmarks' : 'Highlights';
  $('#saved-copy').textContent = type === 'bookmarks' ? 'The passages and places you want to return to.' : 'The lines that made you pause and think.';
  if (type === 'bookmarks' && state.bookmarks.length) {
    $('#saved-content').innerHTML = state.bookmarks.map(mark => {
      const savedBook = books.find(book => book.id === mark.book);
      const savedChapter = savedBook?.chapters?.[mark.chapter]?.title || chapterNames[mark.chapter] || `Section ${mark.chapter + 1}`;
      return `<article class="saved-card"><p>${escapeHtml(savedChapter)}</p><small>${escapeHtml(savedBook?.title || 'Book')} · Chapter ${mark.chapter + 1}</small></article>`;
    }).join('');
  } else {
    $('#saved-content').innerHTML = `<div class="saved-placeholder"><strong>No ${type} yet</strong><span>${type === 'bookmarks' ? 'Tap the bookmark icon while reading to save your place.' : 'Select a memorable passage while reading to keep it here.'}</span></div>`;
  }
  window.scrollTo(0,0);
}

function changeChapter(index) {
  stopSpeech();
  const activeBook = books.find(book => book.id === state.book);
  const total = activeBook?.chapters?.length || 12;
  state.chapter = Math.max(0, Math.min(total - 1, index));
  if (activeBook?.id.startsWith('imported-')) {
    activeBook.progress = Math.round(((state.chapter + 1) / total) * 100);
    saveImportedBook(activeBook).catch(error => console.warn('Reading progress could not be saved', error));
  }
  renderChapter();
  $('#chapter-panel').classList.remove('open');
  window.scrollTo({top: 0, behavior:'smooth'});
}

function applyReaderSettings() {
  readerView.dataset.tone = state.tone;
  document.documentElement.style.setProperty('--reading-size', `${state.fontSize}px`);
  document.documentElement.style.setProperty('--page-width', `${state.width}px`);
  $('#font-value').textContent = state.fontSize;
  $('#width-slider').value = state.width;
  $$('.tone').forEach(button => button.classList.toggle('active', button.dataset.tone === state.tone));
}

function toggleBookmark() {
  const index = state.bookmarks.findIndex(mark => mark.book === state.book && mark.chapter === state.chapter);
  if (index >= 0) { state.bookmarks.splice(index, 1); showToast('Bookmark removed'); }
  else { state.bookmarks.push({book:state.book, chapter:state.chapter}); showToast('Chapter bookmarked'); }
  localStorage.setItem('bookReaderBookmarks', JSON.stringify(state.bookmarks));
  renderChapter();
}

renderBooks();
bindBookMenus();
restoreImportedBooks();
if (!books.some(book => book.id === 'garden')) $('.continue-section').hidden = true;
if (books.find(book => book.id === 'garden')?.progress === 0) {
  $('#featured-book .progress-track span').style.width = '0%';
  $('#featured-book .progress-row strong').textContent = '0%';
}
applyReaderSettings();
$('#featured-book').onclick = () => openReader('garden');
$('#reader-back').onclick = showLibrary;
$('#home-button').onclick = showLibrary;
$('#view-all-button').onclick = () => $('#collection-section').scrollIntoView({behavior:'smooth'});
$('#library-search').oninput = event => renderBooks(event.target.value);
$$('.nav-item').forEach(button => button.onclick = () => button.dataset.view === 'library' ? showLibrary() : showSaved(button.dataset.view));
$('#theme-toggle').onclick = () => { document.body.classList.toggle('dark'); localStorage.setItem('bookReaderDark', document.body.classList.contains('dark')); };
if (localStorage.getItem('bookReaderDark') === 'true') document.body.classList.add('dark');
$('#contents-toggle').onclick = () => $('#chapter-panel').classList.toggle('open');
$('#type-button').onclick = () => { $('#speech-panel').hidden = true; $('#settings-panel').hidden = !$('#settings-panel').hidden; updateSpeechControls(); };
$('#tts-button').onclick = () => {
  if (!('speechSynthesis' in window)) { showToast('Text-to-speech is not available on this device'); return; }
  $('#settings-panel').hidden = true;
  $('#speech-panel').hidden = !$('#speech-panel').hidden;
  updateSpeechControls();
};
$('#speech-play').onclick = toggleSpeech;
$('#speech-stop').onclick = () => stopSpeech();
$('#speech-volume').value = speechState.volume;
$('#speech-volume-value').textContent = `${Math.round(speechState.volume * 100)}%`;
$('#speech-volume').oninput = event => {
  speechState.volume = Number(event.target.value);
  $('#speech-volume-value').textContent = `${Math.round(speechState.volume * 100)}%`;
  localStorage.setItem('bookReaderSpeechVolume', speechState.volume);
  restartSpeechParagraph();
};
$('#speech-voice').onchange = event => {
  localStorage.setItem('bookReaderSpeechVoice', event.target.value);
  restartSpeechParagraph();
};
if ('speechSynthesis' in window) {
  populateDeviceVoices();
  window.speechSynthesis.addEventListener('voiceschanged', populateDeviceVoices);
}
$('#font-down').onclick = () => { state.fontSize = Math.max(14,state.fontSize-1); localStorage.setItem('bookReaderFontSize',state.fontSize); applyReaderSettings(); };
$('#font-up').onclick = () => { state.fontSize = Math.min(25,state.fontSize+1); localStorage.setItem('bookReaderFontSize',state.fontSize); applyReaderSettings(); };
$('#width-slider').oninput = event => { state.width = Number(event.target.value); localStorage.setItem('bookReaderWidth',state.width); applyReaderSettings(); };
$$('.tone').forEach(button => button.onclick = () => { state.tone=button.dataset.tone;localStorage.setItem('bookReaderTone',state.tone);applyReaderSettings(); });
$('#prev-chapter').onclick = () => changeChapter(state.chapter - 1);
$('#next-chapter').onclick = () => changeChapter(state.chapter + 1);
$('#bookmark-button').onclick = toggleBookmark;
$('#reader-search-button').onclick = () => { $('#book-search').hidden = false; $('#book-search-input').focus(); };
$('#close-book-search').onclick = () => { $('#book-search').hidden = true; $('#search-result').textContent = ''; $('#chapter-copy').querySelectorAll('mark').forEach(mark => mark.replaceWith(mark.textContent)); };
$('#book-search-input').oninput = event => {
  const query = event.target.value.trim();
  const copy = $('#chapter-copy');
  copy.querySelectorAll('mark').forEach(mark => mark.replaceWith(mark.textContent));
  if (!query) { $('#search-result').textContent = ''; return; }
  const regex = new RegExp(`(${query.replace(/[.*+?^${}()|[\]\\]/g,'\\$&')})`,'gi');
  let count = 0;
  copy.querySelectorAll('p').forEach(p => { const matches = p.textContent.match(regex); count += matches?.length || 0; p.innerHTML = p.textContent.replace(regex,'<mark>$1</mark>'); });
  $('#search-result').textContent = `${count} match${count === 1 ? '' : 'es'} in this chapter`;
};
$('#import-button').onclick = () => $('#file-input').click();
$('#reset-bookmark').onclick = event => { event.stopPropagation(); resetBookmarkForBook(); };
$('#delete-book').onclick = event => { event.stopPropagation(); deleteBook(); };
document.addEventListener('click', event => { if (!event.target.closest('#book-menu') && !event.target.closest('[data-book-menu]')) closeBookMenu(); });
window.addEventListener('resize', closeBookMenu);
window.addEventListener('scroll', closeBookMenu, {passive:true});
$('#file-input').onchange = event => {
  const file = event.target.files[0];
  if (!file) return;
  importBook(file).catch(error => {
    console.error(error);
    showToast(error.message || 'This book could not be imported');
  }).finally(() => { event.target.value = ''; });
};
document.addEventListener('keydown', event => {
  if (event.key === 'Escape') closeBookMenu();
  if (state.view !== 'reader') return;
  if (event.key === 'ArrowRight') changeChapter(state.chapter + 1);
  if (event.key === 'ArrowLeft') changeChapter(state.chapter - 1);
  if (event.key === 'Escape') { $('#settings-panel').hidden = true; $('#speech-panel').hidden = true; updateSpeechControls(); $('#chapter-panel').classList.remove('open'); }
});
window.addEventListener('beforeunload', () => { if ('speechSynthesis' in window) window.speechSynthesis.cancel(); });
