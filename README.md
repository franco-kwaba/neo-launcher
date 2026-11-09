# Neo Launcher

Launcher Android em Kotlin + Jetpack Compose, com visual neumórfico.

## Estrutura

```
app/src/main/java/dev/neolauncher/
├── MainActivity.kt          # Activity HOME; recarrega apps no onResume
├── LauncherViewModel.kt     # Estado (lista de apps) e ações
├── data/AppRepository.kt    # PackageManager: listar, abrir, info do app
└── ui/
    ├── LauncherScreen.kt    # Home (relógio + dock) e gaveta de apps com busca
    ├── neu/NeuModifier.kt   # Modifier.neu(): sombras saltadas/afundadas
    ├── neu/NeuComponents.kt # NeuSurface, NeuButton, NeuSearchField
    └── theme/NeoTheme.kt    # Paletas clara/escura
```

## Rodar localmente

1. Abra a pasta no Android Studio (ele baixa o Gradle e o SDK que faltarem).
2. **Sem emulador:** abra `LauncherScreen.kt` ou `NeuComponents.kt` e use o painel *Split/Design* para ver os `@Preview`.
3. **Emulador:** Device Manager → criar um Pixel com API 35 → Run ▶.
   Para usar como tela inicial: aperte Home no emulador e escolha *Neo Launcher* → *Sempre*.

## Celular físico

1. Ative *Opções do desenvolvedor* → *Depuração USB* (ou *Depuração por Wi-Fi*).
2. Conecte, aceite a chave RSA e escolha o aparelho no Android Studio → Run ▶.
3. Para voltar ao launcher original: Configurações → Apps → Apps padrão → App de tela inicial.

Gestos: deslizar para cima ou tocar no botão de pontos abre a gaveta; voltar fecha; toque longo abre as informações do app.

## Licença

Código disponível sob a [PolyForm Noncommercial 1.0.0](LICENSE): uso permitido apenas para fins não comerciais.
