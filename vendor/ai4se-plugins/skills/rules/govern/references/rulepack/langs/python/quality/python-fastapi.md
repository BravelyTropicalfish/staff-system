# python-fastapi — FastAPI Rules

## Metadata

| Field | Value |
|---|---|
| **ID** | python-fastapi |
| **Category** | quality |
| **Priority** | High |
| **Activation** | auto |
| **Applies To** | all |
| **Stack** | python |

## Rule

Use these rules for FastAPI projects alongside the general Python rules.

## Structure

- Put app construction in `create_app()`.
- Keep routers thin; move persistence and business behavior into services or CRUD helpers.
- Keep request schemas, update schemas, and response schemas separate.
- Keep database sessions and auth in dependencies.

## Async

- Use `async def` for endpoints that perform I/O.
- Use async database and HTTP clients from async endpoints.
- Do not call `requests`, sync SQLAlchemy sessions, or blocking file/network operations from async routes.

## Dependency Injection

```python
@router.get("/users/{user_id}")
async def get_user(
    user_id: str,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    ...
```

Do not create `SessionLocal()` or long-lived clients inside route handlers.

## Schemas

- Never include passwords, password hashes, access tokens, refresh tokens, or internal auth state in response models.
- Use `response_model` on endpoints that return application data.
- Use field constraints instead of hand-written validation when Pydantic can express the rule.

## Security

- Keep CORS origins environment-specific.
- Do not combine wildcard origins with credentialed CORS.
- Validate JWT expiry, issuer, audience, and algorithm.
- Rate-limit auth and write-heavy endpoints.
- Redact credentials, cookies, authorization headers, and tokens from logs.

## Testing

- Override the exact dependency used by `Depends`.
- Clear `app.dependency_overrides` after tests.
- Prefer async test clients for async applications.

## Rationale

Stack-specific fastapi norms for `python` keep agent output aligned with the project's chosen stack without duplicating common Rules.

## Verification

| Check | Blocks |
|---|---|
| PR review against this rule when touching `python` sources | pr-merge |

## Examples

See Good/Bad (or CORRECT/WRONG) samples in the Rule section when present.

## Exceptions

- None unless recorded in the project decision log / org policy.
