#!/bin/bash
set -e

# Update and install dependencies
echo "Updating system packages..."
sudo apt update && sudo apt upgrade -y

# Install Docker
echo "Installing Docker..."
sudo apt install docker.io -y
sudo systemctl start docker
sudo systemctl enable docker

# Install Docker Compose
echo "Installing Docker Compose..."
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Add user to docker group (requires re-login to take effect, but script continues)
echo "Adding user to docker group..."
sudo usermod -aG docker $USER || true

# Install Nginx
echo "Installing Nginx..."
sudo apt install nginx -y

# Setup Nginx configuration
echo "Configuring Nginx..."
sudo cp nginx.conf /etc/nginx/sites-available/tpa
sudo ln -sf /etc/nginx/sites-available/tpa /etc/nginx/sites-enabled/
sudo rm -f /etc/nginx/sites-enabled/default

# Test Nginx and restart
sudo nginx -t
sudo systemctl restart nginx

# Setup Firewall
echo "Configuring UFW Firewall..."
sudo ufw allow OpenSSH
sudo ufw allow 'Nginx Full'
sudo ufw allow 8080/tcp # allow direct backend access if needed, optional
sudo ufw allow 3000/tcp # allow direct frontend access if needed, optional
sudo ufw --force enable

# Copy .env.template to .env if it doesn't exist
if [ ! -f .env ]; then
    cp .env.template .env
    echo "Please update the .env file with your OPENAI_API_KEY before starting the containers."
fi

echo "Deployment script finished! Please update .env and then run:"
echo "sudo docker-compose up -d --build"
