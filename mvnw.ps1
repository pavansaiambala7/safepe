param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$MavenArgs
)

$toolsDir = "$PSScriptRoot\.tools"
$javaHome = "$toolsDir\jdk17"
$mvnBin = "$toolsDir\maven\bin"

if (!(Test-Path $javaHome)) {
    # Check if a subfolder exists
    $sub = Get-ChildItem "$toolsDir" -Filter "jdk*" -Directory | Select-Object -First 1
    if ($sub) { $javaHome = $sub.FullName }
}

$env:JAVA_HOME = $javaHome
$env:PATH = "$javaHome\bin;$mvnBin;$env:PATH"

& "$mvnBin\mvn.cmd" @MavenArgs
