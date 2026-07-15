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

const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => [...document.querySelectorAll(selector)];
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

function renderBooks(query = '') {
  const visible = books.filter(book => `${book.title} ${book.author}`.toLowerCase().includes(query.toLowerCase()));
  $('#book-grid').innerHTML = visible.map(book => `
    <article class="book-card" data-open-book="${book.id}" tabindex="0" role="button" aria-label="Read ${book.title}">
      <div class="book-cover ${book.cover}"><button class="cover-menu-button" data-book-menu="${book.id}" aria-label="Book options for ${book.title}" title="Book options">•••</button><span class="mini-kicker">${book.kicker}</span><strong>${book.title.toUpperCase().replace(' ', '<br>')}</strong><small>${book.author.toUpperCase()}</small></div>
      <h3>${book.title}</h3><p>${book.author}</p>
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
  const isGarden = book.id === 'garden';
  const title = isGarden ? chapterNames[state.chapter] : state.chapter === 0 ? 'Begin Here' : `Part ${state.chapter + 1}`;
  const paragraphs = getChapterParagraphs(state.chapter);
  $('#reader-title').textContent = book.title;
  $('#reader-author').textContent = book.author;
  $('#chapter-number').textContent = isGarden ? `CHAPTER ${numberWord(state.chapter + 1)}` : `SECTION ${state.chapter + 1}`;
  $('#chapter-title').textContent = title;
  $('#chapter-deck').textContent = isGarden ? chapterDecks[state.chapter] : 'A quiet place for the next page to begin.';
  $('#chapter-copy').innerHTML = paragraphs.map(p => `<p>${p}</p>`).join('');
  $('#page-number').textContent = state.chapter + 1;
  $('#reader-progress-bar').style.width = `${((state.chapter + 1) / 12) * 100}%`;
  $('#prev-chapter').disabled = state.chapter === 0;
  $('#next-chapter').disabled = state.chapter === 11;
  $('#chapter-list').innerHTML = chapterNames.map((name, index) => `<li class="${index === state.chapter ? 'active' : ''}"><button data-chapter="${index}">${String(index + 1).padStart(2,'0')} &nbsp; ${name}</button></li>`).join('');
  $$('[data-chapter]').forEach(button => button.onclick = () => changeChapter(Number(button.dataset.chapter)));
  $('#bookmark-button').classList.toggle('active', state.bookmarks.some(mark => mark.book === state.book && mark.chapter === state.chapter));
  localStorage.setItem('bookReaderChapter', state.chapter);
}

function numberWord(number) {
  return ['ONE','TWO','THREE','FOUR','FIVE','SIX','SEVEN','EIGHT','NINE','TEN','ELEVEN','TWELVE'][number - 1];
}

function openReader(bookId) {
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
  state.view = 'library';
  libraryView.hidden = false;
  savedView.hidden = true;
  readerView.hidden = true;
  $('.topbar').hidden = false;
  $$('.nav-item').forEach(item => item.classList.toggle('active', item.dataset.view === 'library'));
  window.scrollTo(0, 0);
}

function showSaved(type) {
  state.view = type;
  libraryView.hidden = true;
  readerView.hidden = true;
  savedView.hidden = false;
  $('.topbar').hidden = false;
  $$('.nav-item').forEach(item => item.classList.toggle('active', item.dataset.view === type));
  $('#saved-title').textContent = type === 'bookmarks' ? 'Bookmarks' : 'Highlights';
  $('#saved-copy').textContent = type === 'bookmarks' ? 'The passages and places you want to return to.' : 'The lines that made you pause and think.';
  if (type === 'bookmarks' && state.bookmarks.length) {
    $('#saved-content').innerHTML = state.bookmarks.map(mark => `<article class="saved-card"><p>${chapterNames[mark.chapter]}</p><small>${books.find(b => b.id === mark.book)?.title || 'Book'} · Chapter ${mark.chapter + 1}</small></article>`).join('');
  } else {
    $('#saved-content').innerHTML = `<div class="saved-placeholder"><strong>No ${type} yet</strong><span>${type === 'bookmarks' ? 'Tap the bookmark icon while reading to save your place.' : 'Select a memorable passage while reading to keep it here.'}</span></div>`;
  }
  window.scrollTo(0,0);
}

function changeChapter(index) {
  state.chapter = Math.max(0, Math.min(11, index));
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
$('#type-button').onclick = () => { $('#settings-panel').hidden = !$('#settings-panel').hidden; };
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
  const reader = new FileReader();
  reader.onload = () => {
    const title = file.name.replace(/\.txt$/i,'').replace(/[-_]/g,' ');
    books.unshift({id:`imported-${Date.now()}`,title,author:'Imported text',cover:'cover-hours',progress:0,kicker:'MY BOOK'});
    renderBooks();
    showToast(`“${title}” added to your library`);
  };
  reader.readAsText(file);
};
document.addEventListener('keydown', event => {
  if (event.key === 'Escape') closeBookMenu();
  if (state.view !== 'reader') return;
  if (event.key === 'ArrowRight') changeChapter(state.chapter + 1);
  if (event.key === 'ArrowLeft') changeChapter(state.chapter - 1);
  if (event.key === 'Escape') { $('#settings-panel').hidden = true; $('#chapter-panel').classList.remove('open'); }
});
