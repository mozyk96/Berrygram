$ErrorActionPreference = 'Stop'
$global:BerrygramUpstreamFixtureScenario = 'unchanged'
function Invoke-RestMethod {
    param($Headers, $Uri)
    if ($Uri -like '*commits/master') {
        if ($global:BerrygramUpstreamFixtureScenario -eq 'unchanged') { return @{sha='7be672ead283d022812d75c729d3a04dd4c2a2c6'} }
        $message = if ($global:BerrygramUpstreamFixtureScenario -eq 'ahead') { 'update to 13.2.2 (7303)' } else { 'experimental development snapshot' }
        return @{sha=('a' * 40); commit=@{message=$message}}
    }
    if ($Uri -like '*gradle.properties') { return "APP_VERSION_NAME=13.2.2`nAPP_VERSION_CODE=7303" }
    if ($Uri -like '*compare*') { return @{status='ahead'} }
    throw 'Unexpected network call in fixture'
}
$checker = Join-Path $PSScriptRoot '../tools/check-telegram-upstream.ps1'
foreach ($entry in @(@('unchanged','unchanged'), @('ahead','newer_source'), @('experimental','review_required'))) {
    $global:BerrygramUpstreamFixtureScenario = $entry[0]
    $actual = & $checker | ConvertFrom-Json
    if ($actual.status -ne $entry[1]) { throw "Upstream fixture $($entry[0]) returned $($actual.status)" }
}
Write-Host 'Telegram upstream check fixtures passed'
