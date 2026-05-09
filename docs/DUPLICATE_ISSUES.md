# Duplicate GitHub Issues

This document tracks duplicate GitHub issues that should be closed by a maintainer or collaborator with issue triage/write permissions.

Agent permissions did not allow closing issues via the GitHub API in this context, so these duplicates are documented here for follow-up action.

## Duplicates to Close

### Issue #275 - Duplicate of #274

- **Title**: test: desktop USB descriptor and capability capture matrix
- **Status**: OPEN (should be CLOSED)
- **Duplicate of**: #274
- **Reason**: Both issues contain identical content about desktop USB descriptor and capability capture matrix testing.
- **Action**: Close #275 with comment: "Closing as duplicate of #274. Both issues contain identical content."

### Issue #271 - Duplicate of #272

- **Title**: plan: classify Pixel 10 USB modes for companion and sensor use
- **Status**: OPEN (should be CLOSED)
- **Duplicate of**: #272
- **Reason**: Issue #272 contains the same content as #271 plus additional OPSEC guidelines and serial redaction requirements. The OPSEC note in #272 states "Device serial is intentionally obfuscated in this issue. Use `PX10-****-002TF` in planning/docs." Issue #272 is the more complete and security-conscious version.
- **Action**: Close #271 with comment: "Closing as duplicate of #272. Issue #272 contains the same content plus additional OPSEC guidelines and serial redaction requirements, making it the more complete version."

## Verification Commands

To verify these issues are still duplicates:

```bash
gh issue view 274 --repo dvntone/wscanplus --json title,state,createdAt,body
gh issue view 275 --repo dvntone/wscanplus --json title,state,createdAt,body
gh issue view 271 --repo dvntone/wscanplus --json title,state,createdAt,body
gh issue view 272 --repo dvntone/wscanplus --json title,state,createdAt,body
```

To close the duplicates (requires issue triage/write/admin permissions):

```bash
gh issue close 275 --repo dvntone/wscanplus --comment "Closing as duplicate of #274. Both issues contain identical content."
gh issue close 271 --repo dvntone/wscanplus --comment "Closing as duplicate of #272. Issue #272 contains the same content plus additional OPSEC guidelines and serial redaction requirements."
```

## Previously Closed Duplicates

These duplicates were found during the audit but are already closed:

- **Issue #238 / #239**: "Fix test-build UI shell and replace untested camera/acoustic capability states" - Both CLOSED
- **Issue #241 / #242**: "ci: update GitHub Actions to Node 24-compatible versions" - Both CLOSED

## Status

- **Date Documented**: 2026-05-09
- **Documented By**: Claude (claude/fix-duplicate-issues branch)
- **Total Duplicates Found**: 4 pairs (8 issues total)
- **Already Closed**: 2 pairs (#238/#239, #241/#242)
- **Need Closing**: 2 pairs (#271/#272, #274/#275)
- **Action Required**: Close issues #275 and #271 if they are still open and still duplicate their listed canonical issues.
