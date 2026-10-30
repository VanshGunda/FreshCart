# FreshCart local setup
# This stores only the Razorpay TEST secret in the current Windows user's
# environment. It does not write the secret to the project directory.

$secret = Read-Host "Enter your Razorpay TEST key secret" -AsSecureString
$bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secret)
try {
    $plain = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
} finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
}

if ([string]::IsNullOrWhiteSpace($plain)) {
    throw "Razorpay TEST secret cannot be empty."
}

[Environment]::SetEnvironmentVariable("APP_RAZORPAY_KEY_SECRET", $plain, "User")
$env:APP_RAZORPAY_KEY_SECRET = $plain

Write-Host ""
Write-Host "FreshCart local Razorpay secret saved for the current Windows user."
Write-Host "You do not need to edit application.properties for this value."
Write-Host "Start the backend with .\start-local-clean.cmd"
