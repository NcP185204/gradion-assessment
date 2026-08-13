# Test Execution Report - AuthService

## 1. Test Summary
* **Total Tests:** 4
* **Passed:** 4
* **Failed:** 0
* **Execution Time:** ~996 ms
* **Coverage:** 100% Classes, 94% Methods, 62% Branch (AuthService)

## 2. Console Output Log
```text
4 tests passed (4 tests total, 996 ms)
  ✓ loginOrRegister_withNewEmail_createsNewUserAndReturnsToken() (979 ms)
  ✓ loginOrRegister_withExistingEmail_loadsExistingUserAndReturnsToken() (5 ms)
  ✓ loginOrRegister_returnedTokenIsNotNullAndNotEmpty() (7 ms)
  ✓ loginOrRegister_withNullEmail_throwsIllegalArgumentException() (5 ms)

Process finished with exit code 0