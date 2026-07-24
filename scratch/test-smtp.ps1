$smtpServer = "smtp.gmail.com"
$smtpPort = 587
$username = "smartats.ai@gmail.com"
$password = "dujwklhrycvexblg"

$tcpConnection = New-Object System.Net.Sockets.TcpClient($smtpServer, $smtpPort)
$tcpStream = $tcpConnection.GetStream()
$sslStream = New-Object System.Net.Security.SslStream($tcpStream)
# We can't easily do STARTTLS via raw TCP client in a short script. 
# Better to use System.Net.Mail.SmtpClient
try {
    $smtpClient = New-Object Net.Mail.SmtpClient($smtpServer, $smtpPort)
    $smtpClient.EnableSsl = $true
    $smtpClient.Credentials = New-Object System.Net.NetworkCredential($username, $password)
    
    # Just sending an email to itself to test auth
    $smtpClient.Send($username, $username, "Test", "Test")
    Write-Host "SMTP SUCCESS"
} catch {
    Write-Host "SMTP ERROR: $_"
}
