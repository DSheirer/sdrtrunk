Auto-generating Java code from SDRPlay API using jextract.

Note: the api headers in the 3.11 folder are equivalent to the 3.08 headers.  The only
difference from the 3.07 headers is the addition of the boolean 'valid' flag to DeviceT structure.

1. Download JDK19 compatible build of jextract JDK: https://jdk.java.net/jextract/
2. If generating on windows platform, modify the sdrplay_api.h to remove the windows
   pieces of the file.  Specifically:
2a. Comment out these lines but leave the *HANDLE typedef:

//#if defined(_M_X64) || defined(_M_IX86) || defined(_M_ARM64)
//#include "windows.h"
//#elif defined (__GNUC__)
typedef void *HANDLE;
//#endif

//#ifndef _SDRPLAY_DLL_QUALIFIER
//#if !defined(STATIC_LIB) && (defined(_M_X64) || defined(_M_IX86) || defined(_M_ARM64))
//#define _SDRPLAY_DLL_QUALIFIER __declspec(dllimport)
//#elif defined(STATIC_LIB) || defined(__GNUC__)
//#define _SDRPLAY_DLL_QUALIFIER
//#endif
//#endif  // _SDRPLAY_DLL_QUALIFIER

2b. Remove the '_SDRPLAY_DLL_QUALIFIER' from in front of the method declarations

2c. Change the 3.11 version string to 3.08 either in the header file, or in the generated java class

3. Open command prompt to the root source code where the generated files should be placed:
..\SDRtrunk\sdrplay-api\src\main\java

4. Generate the code:
c:\Users\Denny\Downloads\jdk-19-ea2-panama\bin\jextract "C:\Users\Denny\git\sdrtrunk\sdrplay-api\api\v3_11\sdrplay_api.h" -l libsdrplay_api --source

5. Do this for all versions of the API.

