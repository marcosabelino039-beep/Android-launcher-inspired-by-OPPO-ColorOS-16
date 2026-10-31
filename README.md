# ColorOS 16 Launcher — protótipo independente

Launcher Android independente inspirado na linguagem visual do OPPO ColorOS 16. Não é um produto OPPO e não contém código ou recursos proprietários da OPPO.

## Versão atual
- Versão: 0.4.2
- applicationId: `com.peladagpt.coloros16launcher`
- Android mínimo: 8.0 (API 26)
- Alvo de compilação: Android 13 (API 33)
- Kotlin, Jetpack Compose e Gradle
- APK de depuração gerado pelo GitHub Actions

## Recursos implementados
- Pode ser selecionado como aplicativo de tela inicial padrão do Android.
- Descoberta dos aplicativos instalados e exibição dos ícones originais.
- Gaveta de aplicativos com pesquisa em tempo real e grade configurável.
- Tela inicial com grade configurável e aplicativos fixados persistentes.
- Pastas padrão e pastas personalizadas: criar, editar, renomear e excluir.
- Ícones com formatos squircle, arredondado e circular.
- Ajustes de tamanho dos ícones da tela inicial, pastas e dock.
- Ajustes de tamanho dos nomes dos aplicativos.
- Mostrar ou ocultar nomes dos aplicativos.
- Dock configurável: quantidade de ícones, tamanho e transparência.
- Transparência e tamanho dos ícones das pastas.
- Papel de parede escolhido pelo seletor de documentos do Android, com nível de escurecimento ajustável de 0% a 70%.
- Relógio, data e indicador de bateria atualizado periodicamente.
- Animações de toque, transições entre telas, gaveta e abertura de pastas.
- Velocidade das animações configurável.
- Gestos de deslizar para abrir e fechar a gaveta.
- Botão Voltar do Android fecha primeiro diálogos, pastas, gaveta e configurações.
- Preferências de personalização salvas localmente.
- Reorganização dos aplicativos fixados com controles para mover um item uma posição à esquerda ou à direita.

## Limitações conhecidas
Este projeto é um launcher de terceiros, não uma ROM nem um port completo do ColorOS. Não substitui a central de controle, tela de bloqueio, barra de status ou configurações do MIUI. O cartão de clima ainda é ilustrativo; widgets Android reais via AppWidgetHost, drag-and-drop livre entre posições, múltiplas páginas da tela inicial, pacotes de ícones e blur de sistema ainda não estão implementados. Alguns gestos e animações do sistema são controlados pelo Android/MIUI e não podem ser reproduzidos integralmente por um launcher.

## Compilação e instalação para teste
O workflow `.github/workflows/android.yml` executa `gradle assembleDebug` e publica o APK como artefato `ColorOS16-Launcher-APK`. Abra a execução mais recente em **Actions**, entre em **Artifacts** e baixe o ZIP. Extraia o APK e instale no Android. Depois, pressione Home e selecione **ColorOS 16 Launcher** como tela inicial.

Para voltar ao launcher anterior, vá em Configurações do Android → Aplicativos → Aplicativos padrão → App de início/Tela inicial e selecione o launcher da Xiaomi.

O artefato de Actions é temporário e expira conforme a política de retenção do GitHub. O APK atual é de depuração, não uma versão assinada para distribuição pública.
