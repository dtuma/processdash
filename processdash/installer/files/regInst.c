#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <WINREG.H>



const char *KEY_PREFIX = "SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\";
const char *OLD_KEY = "{29A8EB6B-5172-4DC6-A8A2-228F135737FE}";
const char *DISPLAY = "DisplayName";
const char *PATH = "UninstallString";
const char *VERSION = "Version";
const char *INST_PATH = "InstallPath";
const char *DATA_PATH = "DataPath";


char *catstr(char *a, char *b) {
	int lenA = strlen(a);
	int lenB = strlen(b);
	char *result = malloc(lenA + lenB + 1);
	result[0] = 0;
	strcat(result, a);
	strcat(result, b);
	return result;
}

char *frobPath(char *path) {
	char *suffix = strrchr(path, '.');
	if (suffix != NULL && strcmpi(suffix, ".jar") == 0) {
		path = catstr("javaw.exe -jar \"", path);
		path = catstr(path, "\"");
	}
	return path;
}


int reg(char *key, char *displayName, char *version, char *path, char *instpath, char *datapath)
{
	HKEY regKey = NULL;
	char *subkey = catstr(KEY_PREFIX, key);
	LONG result;
	DWORD disposition;


	/*
	 * Create or open the appropriate key in the registry
	 */
	result = RegCreateKeyEx(
		HKEY_LOCAL_MACHINE,			// handle of an open key
		subkey,						// address of subkey name
		0,							// reserved
    	"",							// address of class string
    	REG_OPTION_NON_VOLATILE,	// special options flag
		KEY_ALL_ACCESS,				// desired security access
		NULL,						// address of key security structure
		&regKey,					// address of buffer for opened handle
		&disposition				// address of disposition value buffer
   		);
	if (result != ERROR_SUCCESS)
		return 1;


	/*
	 * Store the name and path under the registry key
	 */
	result = RegSetValueEx(
		regKey,					// handle of key to set value for
		DISPLAY,				// address of name of value to set
		0,						// reserved
		REG_SZ,					// flag for value type
		displayName,			// address of value data
		strlen(displayName)+1	// size of value data
		);
	result = RegSetValueEx(
		regKey,					// handle of key to set value for
		VERSION,				// address of name of value to set
		0,						// reserved
		REG_SZ,					// flag for value type
		version,				// address of value data
		strlen(displayName)+1	// size of value data
		);

	path = frobPath(path);
	result = RegSetValueEx(
		regKey,			// handle of key to set value for
		PATH,			// address of name of value to set
		0,				// reserved
		REG_SZ,			// flag for value type
		path,			// address of value data
		strlen(path)+1	// size of value data
		);
	result = RegSetValueEx(
		regKey,			// handle of key to set value for
		INST_PATH,		// address of name of value to set
		0,				// reserved
		REG_SZ,			// flag for value type
		instpath,		// address of value data
		strlen(path)+1	// size of value data
		);
	result = RegSetValueEx(
		regKey,			// handle of key to set value for
		DATA_PATH,		// address of name of value to set
		0,				// reserved
		REG_SZ,			// flag for value type
		datapath,		// address of value data
		strlen(path)+1	// size of value data
		);


	/*
	 * Close the registry key
	 */
	RegCloseKey(regKey);


	/*
	 * Delete the old uninstallation data created by InstallShield
	 */
	subkey = catstr(KEY_PREFIX, OLD_KEY);
	result = RegDeleteKey(
		HKEY_LOCAL_MACHINE,	// handle of open key
		subkey);			// address of name of subkey


	return 0;
}



int unreg(char *key)
{
	HKEY regKey = NULL;
	char *subkey = catstr(KEY_PREFIX, key);
	LONG result;

	result = RegDeleteKey(
		HKEY_LOCAL_MACHINE,	// handle of open key
		subkey);			// address of name of subkey to delete
	//fprintf(stdout, "delete result is %ld\n", result);

	if (result != ERROR_SUCCESS)
		return 1;

	return 0;
}



int main(int argc,char *argv[])
{
	if (argc == 3 && strcmp(argv[1], "-unreg") == 0) {
		unreg(argv[2]);

	} else if (argc == 8 && strcmp(argv[1], "-reg") == 0) {
		reg(argv[2], argv[3], argv[4], argv[5], argv[6], argv[7]);

	}
	return 0;
}

