@echo off
rem *******************************{begin:header}******************************
rem      XML Processing Snippets - https://code.google.com/p/xml-snippets/     
rem ***************************************************************************
rem 
rem      xml-snippets:   XML Processing Snippets 
rem                      with Some Theoretical Considerations
rem
rem      Copyright (C) 2012-2013 Jani Hautamaki <jani.hautamaki@hotmail.com>
rem
rem      Licensed under the terms of GNU General Public License v3.
rem
rem      You should have received a copy of the GNU General Public License v3
rem      along with this program as the file LICENSE.txt; if not, please see
rem      http://www.gnu.org/licenses/gpl-3.0.html
rem
rem ********************************{end:header}*******************************

java -cp %~dp0\..\build.dir\xmlsnippets.jar; xmlsnippets.fida.XidClient %*