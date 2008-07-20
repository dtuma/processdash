// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003 Tuma Solutions, LLC
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 3
// of the License, or (at your option) any later version.
//
// Additional permissions also apply; see the README-license.txt
// file in the project root directory for more information.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <WINREG.H>



int openConfigFile();
int closeConfigFile();
int installJRE();
int installOthers();
char *catstr(char *, char *);
char *fullPath(char *);
char *readConfigLine();
void debug(char *);
int runFile(char *);

char *CONFIG_DIR = "data\\";
int BUFSIZE = 1000;


FILE *config;
char *requiredJavaVersion;
char *javaInstallationExecutable;


// Run the installation process.
int STDCALL WinMain(HINSTANCE h1, HINSTANCE h2, LPSTR l, int i) {
  int result =
    openConfigFile() ||
    installJRE() ||
    installOthers();

  closeConfigFile();

  return result;
}


// open the configuration file.
int openConfigFile() {
  config = fopen(fullPath("config.ini"), "r");
  return (config == NULL);
}

// read a line from the configuration file and return it.
char *readConfigLine() {
  char *result = malloc(BUFSIZE);
  if (!fgets(result, BUFSIZE, config))
    return NULL;

  strtrim(result);
  return result;
}

// close the configuration file
int closeConfigFile() {
  return fclose(config);
}


// return the full path to a file in the data directory.
char *fullPath(char *filename) {
  return catstr(CONFIG_DIR, filename);
}



/*
 * Possibly install the Java Runtime Enviroment
 */

// read the java installation settings from the first two lines of the
// configuration file.
int readJavaSettings() {
  requiredJavaVersion = readConfigLine();
  javaInstallationExecutable = readConfigLine();

  return (requiredJavaVersion && javaInstallationExecutable);
}

// return nonzero if the JRE installation program needs to be run.
int javaNeedsUpdating() {

  LONG result;
  HKEY regKey;
  char* currentVersion = malloc(10);
  DWORD size = 9;

  result = RegOpenKeyEx
    (HKEY_LOCAL_MACHINE,        // handle of an open key
                                // address of name of value to query
     "SOFTWARE\\JavaSoft\\Java Runtime Environment",
     0,                         // reserved
     KEY_READ,                  // security access mask
     &regKey);                  // address of handle of open key

  if (result != ERROR_SUCCESS) {
    debug("failed to open registry key");
    return 1;
  }

  result = RegQueryValueEx
    (regKey,                    // handle of an open key
     "CurrentVersion",          // address of name of value to query
     0,                         // reserved
     NULL,                      // address of buffer for value type
     currentVersion,            // address of data buffer
     &size);                    // address of data buffer size

  if (result != ERROR_SUCCESS) {
    debug("failed to read registry setting");
    return 1;
  }

  if (strcmp(requiredJavaVersion, currentVersion) > 0)
    return 1;

  return 0;
}


// install the Java Runtime Environment if necessary.
int installJRE()
{
  if (!readJavaSettings()) {
    debug("Couldn't read settings");
    return 1;
  }

  if (!javaNeedsUpdating()) {
    debug("up to date");
    return 0;
  }

  runFile(javaInstallationExecutable);


  if (javaNeedsUpdating()) {
    debug("java installation process failed");
    return 1;
  } else {
    debug("java installation process succeeded");
    return 0;
  }
}


// install all other files named in the config file.
int installOthers()
{
  char *installationFile;
  char *msg = malloc(BUFSIZE);
  int result;
  while (installationFile = readConfigLine()) {
    result = runFile(installationFile);
    if (result) {
      sprintf(msg, "'%s' installation failed", installationFile);
      debug(msg);
      return 1;
    } else {
      sprintf(msg, "'%s' installation succeeded", installationFile);
      debug(msg);
    }
  }

  debug("successfully installed all files");

  return 0;
}



/*
 * Misc utility functions
 */

// concatenate two strings
char *catstr(char *a, char *b) {
        int lenA = strlen(a);
        int lenB = strlen(b);
        char *result = malloc(lenA + lenB + 1);
        result[0] = 0;
        strcat(result, a);
        strcat(result, b);
        return result;
}

// return nonzero if string a ends with string b
int endsWith(char* a, char *b) {
  int aLen = strlen(a);
  int bLen = strlen(b);
  if (aLen < bLen) return 0;
  return (strcmpi(a + aLen - bLen, b) == 0);
}

// for debugging purposes, possibly display a message.
void debug(char *message) {
  // MessageBox(NULL, message, "Debug", MB_OK);
}



// run a process; wait for it to end; return the exit value.
int runProcess(char* cmdLine) {
  STARTUPINFO si;
  PROCESS_INFORMATION pi;
  GetStartupInfo(&si);
  CreateProcess(NULL, cmdLine, NULL, NULL, 0, 0, NULL, NULL, &si, &pi);
  WaitForSingleObject(pi.hProcess , INFINITE);

  DWORD exitCode = 0;
  GetExitCodeProcess(pi.hProcess , &exitCode);
  return (int) exitCode;
}

// run an executable jar file or other executable file.
int runFile(char *filename) {
  if (endsWith(filename, ".jar")) {
    char* cmdLine = "javaw -jar \"";
    cmdLine = catstr(cmdLine, fullPath(filename));
    cmdLine = catstr(cmdLine, "\"");
    return runProcess(cmdLine);
  }
  else
    return runProcess(fullPath(filename));
}
