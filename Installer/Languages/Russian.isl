; *** Inno Setup version 6.5.0+ Russian messages for DISMTools ***
;
; To download user-contributed translations of this file, go to:
;   https://jrsoftware.org/files/istrans/
;
; Note: When translating this text, do not add periods (.) to the end of
; messages that didn't have them already, because on those messages Inno
; Setup adds the periods automatically (appending a period would result in
; two periods being displayed).

[LangOptions]
; The following three entries are very important. Be sure to read and
; understand the '[LangOptions] section' topic in the help file.
LanguageName=Русский
LanguageID=$0419
; LanguageCodePage should always be set if possible, even if this file is Unicode
; For English it's set to zero anyway because English only uses ASCII characters
LanguageCodePage=1251
; If the language you are translating to requires special font faces or
; sizes, uncomment any of the following entries and change them accordingly.
;DialogFontName=
;DialogFontSize=9
;DialogFontBaseScaleWidth=7
;DialogFontBaseScaleHeight=15
;WelcomeFontName=Segoe UI
;WelcomeFontSize=14

[Messages]

; *** Application titles
SetupAppTitle=Установка
SetupWindowTitle=Установка - %1
UninstallAppTitle=Удаление
UninstallAppFullTitle=Удаление %1

; *** Misc. common
InformationTitle=Информация
ConfirmTitle=Подтверждение
ErrorTitle=Ошибка

; *** SetupLdr messages
SetupLdrStartupMessage=Будет установлен %1. Продолжить?
LdrCannotCreateTemp=Не удалось создать временный файл. Установка прервана
LdrCannotExecTemp=Не удалось выполнить файл во временной папке. Установка прервана
HelpTextNote=

; *** Startup error messages
LastErrorMessage=%1.%n%nError %2: %3
SetupFileMissing=Файл %1 отсутствует в папке установки. Исправьте проблему или получите новую копию программы.
SetupFileCorrupt=Файлы установки повреждены. Получите новую копию программы.
SetupFileCorruptOrWrongVer=Файлы установки повреждены или несовместимы с этой версией установщика. Исправьте проблему или получите новую копию программы.
InvalidParameter=В командной строке передан недопустимый параметр:%n%n%1
SetupAlreadyRunning=Установщик уже запущен.
WindowsVersionNotSupported=Эта программа не поддерживает версию Windows, установленную на этом компьютере.
WindowsServicePackRequired=This program requires %1 Service Pack %2 or later.
NotOnThisPlatform=This program will not run on %1.
OnlyOnThisPlatform=This program must be run on %1.
OnlyOnTheseArchitectures=This program can only be installed on versions of Windows designed for the following processor architectures:%n%n%1
WinVersionTooLowError=This program requires %1 version %2 or later.
WinVersionTooHighError=This program cannot be installed on %1 version %2 or later.
AdminPrivilegesRequired=Для установки программы необходимо войти в систему с правами администратора.
PowerUserPrivilegesRequired=You must be logged in as an administrator or as a member of the Power Users group when installing this program.
SetupAppRunningError=Установщик обнаружил, что %1 сейчас запущен.%n%nЗакройте все экземпляры программы и нажмите OK для продолжения или Отмена для выхода.
UninstallAppRunningError=Удаление обнаружило, что %1 сейчас запущен.%n%nЗакройте все экземпляры программы и нажмите OK для продолжения или Отмена для выхода.

; *** Startup questions
PrivilegesRequiredOverrideTitle=Select Setup Install Mode
PrivilegesRequiredOverrideInstruction=Select install mode
PrivilegesRequiredOverrideText1=%1 can be installed for all users (requires administrative privileges), or for you only.
PrivilegesRequiredOverrideText2=%1 can be installed for you only, or for all users (requires administrative privileges).
PrivilegesRequiredOverrideAllUsers=Install for &all users
PrivilegesRequiredOverrideAllUsersRecommended=Install for &all users (recommended)
PrivilegesRequiredOverrideCurrentUser=Install for &me only
PrivilegesRequiredOverrideCurrentUserRecommended=Install for &me only (recommended)

; *** Misc. errors
ErrorCreatingDir=Setup was unable to create the directory "%1"
ErrorTooManyFilesInDir=Unable to create a file in the directory "%1" because it contains too many files

; *** Setup common messages
ExitSetupTitle=Выход из установщика
ExitSetupMessage=Установка не завершена. Если выйти сейчас, программа не будет установлена.%n%nВы можете запустить установщик позже, чтобы завершить установку.%n%nВыйти из установщика?
AboutSetupMenuItem=&О программе установки...
AboutSetupTitle=О программе установки
AboutSetupMessage=%1 version %2%n%3%n%n%1 home page:%n%4
AboutSetupNote=
TranslatorNote=

; *** Buttons
ButtonBack=< &Назад
ButtonNext=&Далее >
ButtonInstall=&Установить
ButtonOK=OK
ButtonCancel=Отмена
ButtonYes=&Да
ButtonYesToAll=Да для &всех
ButtonNo=&Нет
ButtonNoToAll=Н&ет для всех
ButtonFinish=&Готово
ButtonBrowse=&Обзор...
ButtonWizardBrowse=О&бзор...
ButtonNewFolder=&Создать папку

; *** "Select Language" dialog messages
SelectLanguageTitle=Выбор языка установки
SelectLanguageLabel=Выберите язык, который будет использоваться во время установки.

; *** Common wizard text
ClickNext=Нажмите Далее, чтобы продолжить, или Отмена, чтобы выйти из установщика.
BeveledLabel=
BrowseDialogTitle=Выбор папки
BrowseDialogLabel=Выберите папку в списке ниже и нажмите OK.
NewFolderName=Новая папка

; *** "Welcome" wizard page
WelcomeLabel1=Добро пожаловать в мастер установки [name]
WelcomeLabel2=[name/ver] будет установлен на этот компьютер.%n%nПеред продолжением рекомендуется закрыть все остальные приложения.

; *** "Password" wizard page
WizardPassword=Пароль
PasswordLabel1=This installation is password protected.
PasswordLabel3=Please provide the password, then click Next to continue. Passwords are case-sensitive.
PasswordEditLabel=&Пароль:
IncorrectPassword=The password you entered is not correct. Please try again.

; *** "License Agreement" wizard page
WizardLicense=License Agreement
LicenseLabel=Please read the following important information before continuing.
LicenseLabel3=Please read the following License Agreement. You must accept the terms of this agreement before continuing with the installation.
LicenseAccepted=I &accept the agreement
LicenseNotAccepted=I &do not accept the agreement

; *** "Information" wizard pages
WizardInfoBefore=Information
InfoBeforeLabel=Please read the following important information before continuing.
InfoBeforeClickLabel=When you are ready to continue with Setup, click Next.
WizardInfoAfter=Information
InfoAfterLabel=Please read the following important information before continuing.
InfoAfterClickLabel=When you are ready to continue with Setup, click Next.

; *** "User Information" wizard page
WizardUserInfo=User Information
UserInfoDesc=Please enter your information.
UserInfoName=&User Name:
UserInfoOrg=&Organization:
UserInfoSerial=&Serial Number:
UserInfoNameRequired=You must enter a name.

; *** "Select Destination Location" wizard page
WizardSelectDir=Select Destination Location
SelectDirDesc=Где установить [name]?
SelectDirLabel3=Установщик установит [name] в следующую папку.
SelectDirBrowseLabel=Чтобы продолжить, нажмите Далее. Чтобы выбрать другую папку, нажмите Обзор.
DiskSpaceGBLabel=Для установки требуется как минимум [gb] ГБ свободного места.
DiskSpaceMBLabel=Для установки требуется как минимум [mb] МБ свободного места.
CannotInstallToNetworkDrive=Setup cannot install to a network drive.
CannotInstallToUNCPath=Setup cannot install to a UNC path.
InvalidPath=You must enter a full path with drive letter; for example:%n%nC:\APP%n%nor a UNC path in the form:%n%n\\server\share
InvalidDrive=The drive or UNC share you selected does not exist or is not accessible. Please select another.
DiskSpaceWarningTitle=Not Enough Disk Space
DiskSpaceWarning=Setup requires at least %1 KB of free space to install, but the selected drive only has %2 KB available.%n%nDo you want to continue anyway?
DirNameTooLong=The folder name or path is too long.
InvalidDirName=The folder name is not valid.
BadDirName32=Folder names cannot include any of the following characters:%n%n%1
DirExistsTitle=Folder Exists
DirExists=The folder:%n%n%1%n%nalready exists. Would you like to install to that folder anyway?
DirDoesntExistTitle=Folder Does Not Exist
DirDoesntExist=The folder:%n%n%1%n%ndoes not exist. Would you like the folder to be created?

; *** "Select Components" wizard page
WizardSelectComponents=Select Components
SelectComponentsDesc=Какие компоненты установить?
SelectComponentsLabel2=Выберите компоненты для установки. Снимите выбор с компонентов, которые устанавливать не нужно. Нажмите Далее для продолжения.
FullInstallation=Full installation
; if possible don't translate 'Compact' as 'Minimal' (I mean 'Minimal' in your language)
CompactInstallation=Compact installation
CustomInstallation=Custom installation
NoUninstallWarningTitle=Components Exist
NoUninstallWarning=Setup has detected that the following components are already installed on your computer:%n%n%1%n%nDeselecting these components will not uninstall them.%n%nWould you like to continue anyway?
ComponentSize1=%1 KB
ComponentSize2=%1 MB
ComponentsDiskSpaceGBLabel=Current selection requires at least [gb] GB of disk space.
ComponentsDiskSpaceMBLabel=Current selection requires at least [mb] MB of disk space.

; *** "Select Additional Tasks" wizard page
WizardSelectTasks=Select Additional Tasks
SelectTasksDesc=Какие дополнительные задачи выполнить?
SelectTasksLabel2=Выберите дополнительные задачи, которые нужно выполнить при установке [name], затем нажмите Далее.

; *** "Select Start Menu Folder" wizard page
WizardSelectProgramGroup=Select Start Menu Folder
SelectStartMenuFolderDesc=Where should Setup place the program's shortcuts?
SelectStartMenuFolderLabel3=Setup will create the program's shortcuts in the following Start Menu folder.
SelectStartMenuFolderBrowseLabel=To continue, click Next. If you would like to select a different folder, click Browse.
MustEnterGroupName=You must enter a folder name.
GroupNameTooLong=The folder name or path is too long.
InvalidGroupName=The folder name is not valid.
BadGroupName=The folder name cannot include any of the following characters:%n%n%1
NoProgramGroupCheck2=&Don't create a Start Menu folder

; *** "Ready to Install" wizard page
WizardReady=Ready to Install
ReadyLabel1=Установщик готов начать установку [name] на ваш компьютер.
ReadyLabel2a=Нажмите Установить, чтобы продолжить, или Назад, чтобы изменить параметры.
ReadyLabel2b=Click Install to continue with the installation.
ReadyMemoUserInfo=User information:
ReadyMemoDir=Destination location:
ReadyMemoType=Setup type:
ReadyMemoComponents=Selected components:
ReadyMemoGroup=Start Menu folder:
ReadyMemoTasks=Additional tasks:

; *** TDownloadWizardPage wizard page and DownloadTemporaryFile
DownloadingLabel2=Downloading files...
ButtonStopDownload=&Stop download
StopDownload=Are you sure you want to stop the download?
ErrorDownloadAborted=Download aborted
ErrorDownloadFailed=Download failed: %1 %2
ErrorDownloadSizeFailed=Getting size failed: %1 %2
ErrorProgress=Invalid progress: %1 of %2
ErrorFileSize=Invalid file size: expected %1, found %2

; *** TExtractionWizardPage wizard page and ExtractArchive
ExtractingLabel=Extracting files...
ButtonStopExtraction=&Stop extraction
StopExtraction=Are you sure you want to stop the extraction?
ErrorExtractionAborted=Extraction aborted
ErrorExtractionFailed=Extraction failed: %1

; *** Archive extraction failure details
ArchiveIncorrectPassword=The password is incorrect
ArchiveIsCorrupted=The archive is corrupted
ArchiveUnsupportedFormat=The archive format is unsupported

; *** "Preparing to Install" wizard page
WizardPreparing=Preparing to Install
PreparingDesc=Setup is preparing to install [name] on your computer.
PreviousInstallNotCompleted=The installation/removal of a previous program was not completed. You will need to restart your computer to complete that installation.%n%nAfter restarting your computer, run Setup again to complete the installation of [name].
CannotContinue=Setup cannot continue. Please click Cancel to exit.
ApplicationsFound=The following applications are using files that need to be updated by Setup. It is recommended that you allow Setup to automatically close these applications.
ApplicationsFound2=The following applications are using files that need to be updated by Setup. It is recommended that you allow Setup to automatically close these applications. After the installation has completed, Setup will attempt to restart the applications.
CloseApplications=&Automatically close the applications
DontCloseApplications=&Do not close the applications
ErrorCloseApplications=Setup was unable to automatically close all applications. It is recommended that you close all applications using files that need to be updated by Setup before continuing.
PrepareToInstallNeedsRestart=Setup must restart your computer. After restarting your computer, run Setup again to complete the installation of [name].%n%nWould you like to restart now?

; *** "Installing" wizard page
WizardInstalling=Installing
InstallingLabel=Подождите, пока [name] устанавливается на ваш компьютер.

; *** "Setup Completed" wizard page
FinishedHeadingLabel=Завершение мастера установки [name]
FinishedLabelNoIcons=Установка [name] на компьютер завершена.
FinishedLabel=Установка [name] на компьютер завершена. Приложение можно запустить через установленные ярлыки.
ClickFinish=Click Finish to exit Setup.
FinishedRestartLabel=To complete the installation of [name], Setup must restart your computer. Would you like to restart now?
FinishedRestartMessage=To complete the installation of [name], Setup must restart your computer.%n%nWould you like to restart now?
ShowReadmeCheck=Yes, I would like to view the README file
YesRadio=&Yes, restart the computer now
NoRadio=&No, I will restart the computer later
; used for example as 'Run MyProg.exe'
RunEntryExec=Run %1
; used for example as 'View Readme.txt'
RunEntryShellExec=View %1

; *** "Setup Needs the Next Disk" stuff
ChangeDiskTitle=Setup Needs the Next Disk
SelectDiskLabel2=Please insert Disk %1 and click OK.%n%nIf the files on this disk can be found in a folder other than the one displayed below, enter the correct path or click Browse.
PathLabel=&Path:
FileNotInDir2=The file "%1" could not be located in "%2". Please insert the correct disk or select another folder.
SelectDirectoryLabel=Please specify the location of the next disk.

; *** Installation phase messages
SetupAborted=Setup was not completed.%n%nPlease correct the problem and run Setup again.
AbortRetryIgnoreSelectAction=Select action
AbortRetryIgnoreRetry=&Try again
AbortRetryIgnoreIgnore=&Ignore the error and continue
AbortRetryIgnoreCancel=Cancel installation
RetryCancelSelectAction=Select action
RetryCancelRetry=&Try again
RetryCancelCancel=Cancel

; *** Installation status messages
StatusClosingApplications=Closing applications...
StatusCreateDirs=Создание папок...
StatusExtractFiles=Извлечение файлов...
StatusDownloadFiles=Downloading files...
StatusCreateIcons=Создание ярлыков...
StatusCreateIniEntries=Создание записей INI...
StatusCreateRegistryEntries=Создание записей реестра...
StatusRegisterFiles=Регистрация файлов...
StatusSavingUninstall=Сохранение информации для удаления...
StatusRunProgram=Завершение установки...
StatusRestartingApplications=Restarting applications...
StatusRollback=Rolling back changes...

; *** Misc. errors
ErrorInternal2=Internal error: %1
ErrorFunctionFailedNoCode=%1 failed
ErrorFunctionFailed=%1 failed; code %2
ErrorFunctionFailedWithMessage=%1 failed; code %2.%n%3
ErrorExecutingProgram=Unable to execute file:%n%1

; *** Registry errors
ErrorRegOpenKey=Error opening registry key:%n%1\%2
ErrorRegCreateKey=Error creating registry key:%n%1\%2
ErrorRegWriteKey=Error writing to registry key:%n%1\%2

; *** INI errors
ErrorIniEntry=Error creating INI entry in file "%1".

; *** File copying errors
FileAbortRetryIgnoreSkipNotRecommended=&Skip this file (not recommended)
FileAbortRetryIgnoreIgnoreNotRecommended=&Ignore the error and continue (not recommended)
SourceIsCorrupted=The source file is corrupted
SourceDoesntExist=The source file "%1" does not exist
SourceVerificationFailed=Verification of the source file failed: %1
VerificationSignatureDoesntExist=The signature file "%1" does not exist
VerificationSignatureInvalid=The signature file "%1" is invalid
VerificationKeyNotFound=The signature file "%1" uses an unknown key
VerificationFileNameIncorrect=The name of the file is incorrect
VerificationFileTagIncorrect=The tag of the file is incorrect
VerificationFileSizeIncorrect=The size of the file is incorrect
VerificationFileHashIncorrect=The hash of the file is incorrect
ExistingFileReadOnly2=The existing file could not be replaced because it is marked read-only.
ExistingFileReadOnlyRetry=&Remove the read-only attribute and try again
ExistingFileReadOnlyKeepExisting=&Keep the existing file
ErrorReadingExistingDest=An error occurred while trying to read the existing file:
FileExistsSelectAction=Select action
FileExists2=The file already exists.
FileExistsOverwriteExisting=&Overwrite the existing file
FileExistsKeepExisting=&Keep the existing file
FileExistsOverwriteOrKeepAll=&Do this for the next conflicts
ExistingFileNewerSelectAction=Select action
ExistingFileNewer2=The existing file is newer than the one Setup is trying to install.
ExistingFileNewerOverwriteExisting=&Overwrite the existing file
ExistingFileNewerKeepExisting=&Keep the existing file (recommended)
ExistingFileNewerOverwriteOrKeepAll=&Do this for the next conflicts
ErrorChangingAttr=An error occurred while trying to change the attributes of the existing file:
ErrorCreatingTemp=An error occurred while trying to create a file in the destination directory:
ErrorReadingSource=An error occurred while trying to read the source file:
ErrorCopying=An error occurred while trying to copy a file:
ErrorDownloading=An error occurred while trying to download a file:
ErrorExtracting=An error occurred while trying to extract an archive:
ErrorReplacingExistingFile=An error occurred while trying to replace the existing file:
ErrorRestartReplace=RestartReplace failed:
ErrorRenamingTemp=An error occurred while trying to rename a file in the destination directory:
ErrorRegisterServer=Unable to register the DLL/OCX: %1
ErrorRegSvr32Failed=RegSvr32 failed with exit code %1
ErrorRegisterTypeLib=Unable to register the type library: %1

; *** Uninstall display name markings
; used for example as 'My Program (32-bit)'
UninstallDisplayNameMark=%1 (%2)
; used for example as 'My Program (32-bit, All users)'
UninstallDisplayNameMarks=%1 (%2, %3)
UninstallDisplayNameMark32Bit=32-bit
UninstallDisplayNameMark64Bit=64-bit
UninstallDisplayNameMarkAllUsers=All users
UninstallDisplayNameMarkCurrentUser=Current user

; *** Post-installation errors
ErrorOpeningReadme=Произошла ошибка при попытке открыть файл README.
ErrorRestartingComputer=Установщик не смог перезапустить компьютер. Сделайте это вручную.

; *** Uninstaller messages
UninstallNotFound=Файл "%1" не существует. Удаление невозможно.
UninstallOpenError=Файл "%1" не удалось открыть. Удаление невозможно
UninstallUnsupportedVer=Файл журнала удаления "%1" имеет формат, который не распознан этой версией деинсталлятора. Удаление невозможно
UninstallUnknownEntry=В журнале удаления обнаружена неизвестная запись (%1)
ConfirmUninstall=Вы действительно хотите полностью удалить %1 и все его компоненты?
UninstallOnlyOnWin64=Эту установку можно удалить только в 64 битной Windows.
OnlyAdminCanUninstall=Эту установку может удалить только пользователь с правами администратора.
UninstallStatusLabel=Подождите, пока %1 удаляется с компьютера.
UninstalledAll=%1 успешно удалён с компьютера.
UninstalledMost=Удаление %1 завершено.%n%nНекоторые элементы удалить не удалось. Их можно удалить вручную.
UninstalledAndNeedsRestart=Чтобы завершить удаление %1, компьютер нужно перезапустить.%n%nПерезапустить сейчас?
UninstallDataCorrupted=Файл "%1" повреждён. Удаление невозможно

; *** Uninstallation phase messages
ConfirmDeleteSharedFileTitle=Remove Shared File?
ConfirmDeleteSharedFile2=The system indicates that the following shared file is no longer in use by any programs. Would you like for Uninstall to remove this shared file?%n%nIf any programs are still using this file and it is removed, those programs may not function properly. If you are unsure, choose No. Leaving the file on your system will not cause any harm.
SharedFileNameLabel=File name:
SharedFileLocationLabel=Location:
WizardUninstalling=Состояние удаления
StatusUninstalling=Удаление %1...

; *** Shutdown block reasons
ShutdownBlockReasonInstallingApp=Установка %1.
ShutdownBlockReasonUninstallingApp=Удаление %1.

; The custom messages below aren't used by Setup itself, but if you make
; use of them in your scripts, you'll want to translate them.

[CustomMessages]

NameAndVersion=%1 версия %2
AdditionalIcons=Дополнительные ярлыки:
CreateDesktopIcon=Создать ярлык на &рабочем столе
CreateQuickLaunchIcon=Создать ярлык в панели &быстрого запуска
ProgramOnTheWeb=%1 в интернете
UninstallProgram=Удалить %1
LaunchProgram=Запустить %1
AssocFileExtension=&Связать %1 с расширением файлов %2
AssocingFileExtension=Связывание %1 с расширением файлов %2...
AutoStartProgramGroupDescription=Автозагрузка:
AutoStartProgram=Автоматически запускать %1
AddonHostProgramNotFound=%1 не найден в выбранной папке.%n%nПродолжить всё равно?
InstallerWord=Установщик
UninstallerWord=Деинсталлятор
AutoReloadTaskDescription=Установить службу автоматической перезагрузки образов
ServicesTaskGroup=Службы
AutoReloadServiceDisplayName=DISMTools Automatic Image Reload Service
AutoReloadServiceDescription=Эта служба автоматически перезагружает сеансы обслуживания всех смонтированных образов на этом компьютере. Её можно отключить, если она не нужна.
