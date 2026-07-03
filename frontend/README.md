# JL Enterprises — Static Frontend

Plain HTML/CSS/JS store + staff admin. No build step — deploys to Vercel as a
static site. Talks to the backend (`../ecommerce-backend`) over `/api/v1`.

## Contents
- Storefront: `index.html`, category pages (`air-conditioners.html`, …), `cart.html`,
  `login.html`, `signup.html`, `service.html`, `track-order.html`, legal pages, etc.
- Shared: `style.css`, `script.js`.
- Admin (bearer-token auth against the backend): `admin-login.html`, `admin.html`,
  `admin-team.html`, `admin.js`.

## Deploy to Vercel
1. Push the repo, then in Vercel: **New Project → Import** this repo.
2. Set **Root Directory** to `frontend`.
3. Framework preset: **Other** (static). No build command, output = the folder itself.
4. Deploy. Pages are served at `/index.html`, `/admin-login.html`, etc.

## Point the admin at your backend
Edit [`admin.js`](admin.js) and set the production API URL:
```js
// non-localhost branch:
return "https://jl-ecommerce-api.onrender.com";   // your Render backend URL
```
Then add this site's Vercel origin (e.g. `https://jlenterprises.vercel.app`) to the
backend's `CORS_ORIGINS` env var so the browser calls are allowed.

## Local preview
Any static server, e.g.:
```bash
cd frontend
python -m http.server 5500
# open http://localhost:5500/index.html
```
With the backend running on `localhost:8081`, `admin.js` auto-targets it in dev.
