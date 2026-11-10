# Empacotamento multiplataforma

Os pacotes precisam ser gerados no sistema operacional de destino. Isso é necessário porque JavaFX e o runtime produzido pelo `jpackage` contêm bibliotecas nativas específicas de cada plataforma e arquitetura.

## Linux

```bash
./packaging/build-package.sh
./packaging/build-package.sh deb
./packaging/build-package.sh rpm
```

Sem argumento, é criada uma aplicação portátil `.tar.gz`. A geração de `.deb` exige as ferramentas Debian (`dpkg-deb` e `fakeroot`); a geração de `.rpm` exige `rpmbuild`.

## macOS

```bash
./packaging/build-package.sh
./packaging/build-package.sh app-image
./packaging/build-package.sh pkg
```

Sem argumento, é criado um `.dmg`. O pacote gerado localmente não é assinado nem notarizado; para distribuição pública fora da equipe, será necessário usar um certificado Apple Developer ID e o serviço de notarização da Apple.

Gere um pacote em cada arquitetura que será suportada (`arm64` para Apple Silicon e `x86_64` para Macs Intel), ou execute o build em máquinas/runners separados.

## Windows

```powershell
powershell -ExecutionPolicy Bypass -File packaging\windows\build-installer.ps1
powershell -ExecutionPolicy Bypass -File packaging\windows\build-installer.ps1 -PackageType exe
powershell -ExecutionPolicy Bypass -File packaging\windows\build-installer.ps1 -PackageType app-image
```

O tipo padrão é `.msi` e requer WiX Toolset.
