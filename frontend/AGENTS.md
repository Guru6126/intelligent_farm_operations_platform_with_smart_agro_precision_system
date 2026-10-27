<!-- LOVABLE:BEGIN -->
> [!IMPORTANT]
> This project is connected to [Lovable](https://lovable.dev). Avoid rewriting
> published git history — force pushing, or rebasing/amending/squashing commits
> that are already pushed — as it rewrites history on Lovable's side and the
> user will likely lose their project history.
>
> Commits you push to the connected branch sync back to Lovable and show up in
> the editor, so keep the branch in a working state.
<!-- LOVABLE:END -->

## Architecture
- Frontend talks only to the Spring Boot REST API in `backend/` via `src/lib/api.ts` (JWT bearer); no other backend — user chose Java + MySQL.
- All authorization and business rules live in the backend services (`AccessService`, `DataService`); the frontend never decides permissions.
- Setup and architecture docs live only in `PROJECT_GUIDE.md`, never in the app UI.
