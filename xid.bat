@echo off
rem *******************************{begin:header}******************************
rem      XML Processing Snippets - https://code.google.com/p/xml-snippets/     
rem ***************************************************************************
rem 
rem      xml-snippets:   XML Processing Snippets 
rem                      with Some Theoretical Considerations
rem
rem      Copyright (C) 2012 Jani Hautamäki <jani.hautamaki@hotmail.com>
rem
rem      Licensed under the terms of GNU General Public License v3.
rem
rem      You should have received a copy of the GNU General Public License v3
rem      along with this program as the file LICENSE.txt; if not, please see
rem      http://www.gnu.org/licenses/gpl-3.0.html
rem
rem ********************************{end:header}*******************************

rem This is a proxy to manage possible changing package of the example codes. 
rem The call is currently passed on to the actual script just as such.
rem In the future, however, it might be possible that there will be a prefix
rem in the parameters.

xmlsnippet.bat xmlsnippets.util.XidDebugger %*