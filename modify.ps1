# Define a timestamp to add as a comment
$timestamp = "# Modified by LukeF's script on $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")"

# Function to modify files
function Modify-Files($path) {
    # Get all files in the current directory
    $items = Get-ChildItem -Path $path

    foreach ($item in $items) {
        if ($item.PSIsContainer) {
            # Recursively call function for subdirectories
            Modify-Files $item.FullName
        }
        else {
            # Read the content of the file
            $content = Get-Content $item.FullName -Raw

            # Append the timestamp as a comment at the end of the file
            $content += "`n$timestamp"

            # Write the modified content back to the file
            Set-Content -Path $item.FullName -Value $content
        }
    }
}

# Start modifying files from the root directory
Modify-Files "C:\Users\lukef\work\parkedcarfinder"

# Modified by LukeF's script on 2024-07-19 22:08:04
