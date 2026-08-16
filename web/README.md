# 🌐 Signet Web (Landing Page, Términos & Privacidad)

Sitio web oficial, documentación y páginas legales de **Signet (Android KeyStore & Certificate Tool)**, optimizado para despliegue estático de alto rendimiento en **Cloudflare Pages**.

---

## ⚡ Stack Tecnológico

- **Framework**: [Astro 5](https://astro.build/) (Static Site Generation con Zero JS runtime por defecto).
- **Estilos**: [Tailwind CSS](https://tailwindcss.com/) con paleta adaptada al tema OLED y Emerald de Signet.
- **Despliegue**: [Cloudflare Pages](https://pages.cloudflare.com/) (distribuido en el Edge global con latencia <50ms).

---

## 📁 Estructura del Módulo Web

```
web/
├── public/
│   ├── favicon.svg             # Ícono SVG oficial de Signet
│   └── robots.txt              # Directivas de indexación limpia
├── src/
│   ├── components/
│   │   ├── Navbar.astro        # Barra de navegación responsiva
│   │   ├── Footer.astro        # Pie de página con enlaces y licencia GPL v3
│   │   ├── Hero.astro          # Sección principal con llamadas a la acción
│   │   ├── Features.astro      # Cuadrícula de características criptográficas
│   │   ├── SecurityPillars.astro # Declaración de Cero Telemetría y Cero Recolección
│   │   └── DownloadSection.astro # Botones de descarga APK, GitHub y Uptodown
│   ├── layouts/
│   │   └── Layout.astro        # Layout base HTML5 con SEO, OpenGraph y tema
│   ├── pages/
│   │   ├── index.astro         # Página de inicio / Landing oficial
│   │   ├── privacy.astro       # Política de Privacidad (Cero Recolección de Datos)
│   │   ├── terms.astro         # Términos y Condiciones (GPL v3 y custodia)
│   │   └── 404.astro           # Página de error 404 personalizada
│   └── styles/
│       └── global.css          # Estilos globales y utilidades
├── astro.config.mjs            # Configuración de Astro con Tailwind
├── tailwind.config.mjs         # Configuración de Tailwind CSS
├── wrangler.toml               # Configuración de Cloudflare Pages
└── package.json                # Dependencias y scripts
```

---

## 🚀 Desarrollo Local

```bash
# Entrar al directorio
cd web

# Instalar dependencias
npm install

# Iniciar servidor de desarrollo
npm run dev

# Compilar para producción (genera la carpeta dist/)
npm run build

# Previsualizar la compilación de producción localmente
npm run preview
```

---

## ☁️ Despliegue en Cloudflare Pages

### Método 1: Conexión Automática con Git (Recomendado)
1. Ve a tu panel de **Cloudflare Dashboard** > **Workers & Pages** > **Create application** > **Pages** > **Connect to Git**.
2. Selecciona tu repositorio de GitHub.
3. Configura los ajustes de compilación:
   - **Framework preset**: `Astro`
   - **Root directory**: `web`
   - **Build command**: `npm run build`
   - **Build output directory**: `dist`
4. Haz clic en **Save and Deploy**. Cloudflare compilará y actualizará el sitio en cada `git push`.

### Método 2: Despliegue Directo con Wrangler CLI
```bash
cd web
npm run build
npx wrangler pages deploy dist --project-name=signet-web
```
