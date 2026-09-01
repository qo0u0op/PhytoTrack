// happy-dom 環境不實作 localStorage (Node 需 --localstorage-file 才提供)，
// 以最小記憶體實作模擬，讓依賴 localStorage 的 store (auth) 可測。
const storage = new Map<string, string>()

const localStorageMock: Storage = {
  get length () {
    return storage.size
  },
  clear: () => storage.clear (),
  getItem: (key) => storage.get (key) ?? null,
  key: (index) => [...storage.keys ()][index] ?? null,
  removeItem: (key) => void storage.delete (key),
  setItem: (key, value) => void storage.set (key, String (value)),
}

Object.defineProperty (globalThis, 'localStorage', {
  value: localStorageMock,
  configurable: true,
  writable: true,
})