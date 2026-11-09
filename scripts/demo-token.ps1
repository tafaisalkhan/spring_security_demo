param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('alice', 'bob', 'ops')]
    [string]$Subject,
    [string]$TokenRole = 'USER'
)

$demoSecret = 'demo-only-secret-change-before-any-deployment'

function ConvertTo-Base64UrlJson($value) {
    $json = ConvertTo-Json -InputObject $value -Compress -Depth 5
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($json)
    return [Convert]::ToBase64String($bytes).TrimEnd('=').Replace('+', '-').Replace('/', '_')
}

$jwtHeader = ConvertTo-Base64UrlJson @{ alg = 'HS256'; typ = 'JWT' }
$jwtPayload = ConvertTo-Base64UrlJson @{
    iss = 'demo'
    sub = $Subject
    exp = [DateTimeOffset]::UtcNow.AddHours(1).ToUnixTimeSeconds()
    realm_access = @{ roles = @($TokenRole) }
}
$signingInput = "$jwtHeader.$jwtPayload"
$hmac = [System.Security.Cryptography.HMACSHA256]::new([System.Text.Encoding]::UTF8.GetBytes($demoSecret))
try {
    $signatureBytes = $hmac.ComputeHash([System.Text.Encoding]::UTF8.GetBytes($signingInput))
    $signature = [Convert]::ToBase64String($signatureBytes).TrimEnd('=').Replace('+', '-').Replace('/', '_')
    Write-Output "$signingInput.$signature"
} finally {
    $hmac.Dispose()
}
