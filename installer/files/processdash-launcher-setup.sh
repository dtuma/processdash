#!/bin/sh

# change to the directory containing this script, so we can use relative filenames
#
BASEDIR=$(dirname "$0")
cd "$BASEDIR"

# install icons for the app
#
xdg-icon-resource install --noupdate --context mimetypes --size 48 --mode user dashicon.png application-x-processdash-personal-shortcut
xdg-icon-resource install --noupdate --context mimetypes --size 48 --mode user teamicon.png application-x-processdash-team-shortcut
xdg-icon-resource install            --context apps      --size 48 --mode user dashicon.png processdash-dashicon

# register the mime types for the app
#
xdg-mime install --mode user processdash-launcher-mime-types.xml

# register an application icon to run the app
#
chmod 755 processdash-launcher.sh
xdg-desktop-menu install --mode user processdash-launcher.desktop
