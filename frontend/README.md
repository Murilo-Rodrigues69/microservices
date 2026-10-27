# NFE Frontend

Frontend administrativo em TypeScript + React + Vite para a API de NF-e e
NFC-e.

## Como rodar

```bash
npm install
npm run dev
```

Abra `http://localhost:5000`.

Por padrao, o Vite encaminha chamadas `/api` para `http://localhost:8081`, o que
evita problemas de CORS no desenvolvimento local. Se quiser chamar outra base
diretamente, crie um `.env.local`:

```bash
VITE_API_BASE_URL=http://localhost:8081
```

## Scripts

```bash
npm run dev
npm run build
npm run preview
```
