# API error contract

All errors produced by the HTTP API use RFC Problem Details and the media type
`application/problem+json`. The common response fields are:

- `status`: HTTP status code;
- `title`: short, stable summary;
- `detail`: safe explanation intended for API clients;
- `instance`: path of the failed request;
- `code`: stable, machine-readable application code.

Validation failures also include an `errors` array. Each entry contains only
`field`, `code` and `message`. Rejected values are deliberately omitted because
they may contain credentials, personal data or other sensitive input.

```json
{
  "type": "about:blank",
  "title": "Validation failed",
  "status": 400,
  "detail": "One or more request fields are invalid",
  "instance": "/api/v1/gamegenre/add",
  "code": "validation_failed",
  "errors": [
    {
      "field": "name",
      "code": "Size",
      "message": "el tamaño tiene que estar entre 4 y 30"
    }
  ]
}
```

Authentication and authorization failures use the same contract even though
they are produced by the Spring Security filter chain. Unexpected exceptions
are logged on the server with their stack trace, while clients receive only the
generic `internal_error` response. Database messages, exception class names,
causes and stack traces must never be serialized to clients.

Application code should throw a meaningful application exception and let the
global handler translate it. Controllers must not build JSON strings manually.
