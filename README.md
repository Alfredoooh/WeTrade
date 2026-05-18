# Pulse App — React Native Web Static

App React Native Web compilada para ficheiros estáticos. Zero servidor em produção.

## Stack
- React Native Web 0.19
- Webpack 5 + Babel (build apenas)
- Output: ficheiros estáticos em `/dist`

## Dev local
```bash
npm install
npm run dev
# http://localhost:3000
```

## Build
```bash
npm install
npm run build
# Gera /dist com index.html + bundle.js
```

## Deploy no Render — Static Site

1. Push do projeto para GitHub/GitLab
2. Render → **New → Static Site**
3. Liga o repositório
4. Preenche:
   - **Build Command:** `npm install && npm run build`
   - **Publish Directory:** `dist`
5. Clica **Create Static Site**

O `render.yaml` já configura tudo automaticamente se usares **New → Blueprint**.

## Ecrãs
| Ecrã | Descrição |
|------|-----------|
| **Home** | Dashboard com métricas animadas e feed live |
| **Explorar** | Catálogo com pesquisa e filtros |
| **Análises** | Gráfico de barras + tabela de páginas |
| **Perfil** | Perfil, stats e definições |
