:: Make the schema documenation for a version of the SPASE data model.
:: Using Oxygen schemaDocumentation tool.
::
:: Author: Todd King
::
copy %2\data\schema\spase-%1.xsd . 
call "C:\Program Files (x86)\Oxygen XML Editor 12\schemaDocumentation.bat" spase-%1.xsd -out:spase-%1.pdf -format:pdf
copy spase-%1.pdf %2\data\model\spase-%1.pdf
copy spase-%1.pdf %2\docs\model\spase-%1.pdf

:: Clean-up
del spase-%1.xsd
del spase-%1.pdf