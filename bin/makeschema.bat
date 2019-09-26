:: Make the schema documentation for a version of the SPASE data model.
:: Using Oxygen schemaDocumentation tool.
::
:: %1 : base file name
:: %2 : Path to root of web site output
::
:: Author: Todd King
::
copy %2\data\schema\%1.xsd . 

:: call "C:\Program Files (x86)\Oxygen XML Editor 13\schemaDocumentation.bat" %1.xsd -out:%1.pdf -format:pdf
call "C:\Program Files\Oxygen XML Editor 16\schemaDocumentation.bat" %1.xsd -out:%1.pdf -format:pdf
copy %1.pdf %2\data\model\%1.pdf
:: copy %1.pdf %2\docs\model\%1.pdf

:: call "C:\Program Files (x86)\Oxygen XML Editor 13\schemaDocumentation.bat" %1.xsd -cfg:oxygen-html-settings.xml
call "C:\Program Files\Oxygen XML Editor 16\schemaDocumentation.bat" %1.xsd -cfg:oxygen-html-settings.xml
rmdir /S /Q %2\data\model\%1
move /Y spase-out %2\data\model\%1

:: Clean-up
del %1.xsd
del %1.pdf