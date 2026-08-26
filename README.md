# NF-e Scanner

Aplicativo Android que lê o código de barras ou QR Code de uma NF-e/NFC-e,
valida a chave de acesso e abre o Portal Nacional da NF-e com o campo já
preenchido.

O aplicativo **não resolve, não clica e não intercepta o hCaptcha**. A validação
humana e o envio da consulta permanecem sob controle do usuário dentro da página
oficial.

## Recursos

- Leitura de Code 128, Code 39, Code 93, ITF e QR Code com CameraX + ML Kit.
- OCR local da chave impressa como alternativa para códigos riscados ou apagados.
- Detector do ML Kit incorporado ao APK; não há download do modelo na primeira leitura.
- Leitura imediata de códigos válidos e confirmação temporal para resultados do OCR.
- Validação dos 44 caracteres, UF, mês, modelo e dígito verificador módulo 11.
- Compatibilidade com chaves numéricas e com CNPJ alfanumérico.
- Digitação/colagem manual como alternativa à câmera.
- Lanterna em aparelhos com flash.
- WebView limitada ao domínio oficial `nfe.fazenda.gov.br`.
- JavaScript habilitado somente porque a página oficial e o hCaptcha precisam dele.
- Sem `addJavascriptInterface`, acesso a arquivos locais ou tráfego HTTP.
- Tema claro/escuro, cores dinâmicas e suporte a rotação.

## Requisitos

- Android Studio compatível com Android Gradle Plugin 8.13.
- JDK 17.
- Android SDK 36.
- Android 6.0 (API 23) ou mais recente no aparelho.

As versões de Kotlin, Compose, Activity e Core estão fixadas na última faixa
compatível com Android 36 e AGP 8.13. As versões seguintes de Compose/Core já
exigem Android 37 e AGP 9.1, portanto não devem ser atualizadas isoladamente.

## Abrir e executar

1. Abra a pasta `NFeScanner` no Android Studio.
2. Aguarde a sincronização do Gradle.
3. Conecte um aparelho Android com depuração USB habilitada.
4. Selecione a configuração `app` e clique em **Run**.

Pelo terminal:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

O APK de desenvolvimento será criado em:

```text
app/build/outputs/apk/debug/app-debug.apk
```

No pacote pronto desta entrega, uma cópia instalável também está em:

```text
release/NFeScanner-debug.apk
```

### Atualização da versão 1.0

A primeira entrega usava uma assinatura `debug` temporária. Se ela já estiver
instalada, desinstale a versão 1.0 antes de instalar a 1.1. Essa troca é
necessária apenas uma vez.

A partir da versão 1.1, o projeto inclui uma chave exclusiva de desenvolvimento
para manter as próximas atualizações compatíveis. Ela é intencionalmente pública
e **não deve ser utilizada para publicar o aplicativo em produção**.

## Fluxo do aplicativo

1. O usuário concede acesso à câmera ou escolhe digitar a chave.
2. O app tenta o código e, se necessário, lê os 44 caracteres impressos.
3. Toda chave encontrada é validada localmente, inclusive pelo dígito verificador.
4. A chave é apresentada para conferência.
5. O usuário abre o Portal Nacional.
6. O app preenche somente o campo de chave de acesso.
7. O usuário conclui o hCaptcha e toca no botão de consulta da própria página.

## Códigos danificados

O Code 128 não possui correção visual de partes apagadas. Quando a leitura das
barras falha, o app usa o reconhecimento de texto incorporado para localizar a
linha de 44 caracteres impressa abaixo do código. Mantenha as barras e os números
visíveis, evite reflexos e aproxime o documento até ele ocupar a maior parte da
mira.

## Chaves alfanuméricas

O parser aceita o padrão atualizado para CNPJ alfanumérico:

```text
[0-9]{6}[A-Z0-9]{12}[0-9]{26}
```

No cálculo do DV, cada caractere é convertido usando `ASCII - 48` e depois é
aplicado módulo 11 com pesos de 2 a 9, da direita para a esquerda.

## Manutenção da integração com o portal

A página do Portal Nacional não oferece uma API pública para preenchimento do
formulário. O app procura primeiro o campo atual
`txtChaveAcessoResumo` e depois utiliza um fallback seguro para um campo de texto
com 44 posições.

Se o Portal alterar o HTML, o usuário verá um aviso e ainda poderá colar a chave
manualmente. A lógica fica em:

```text
app/src/main/java/br/com/aydasoft/nfescanner/ui/screens/PortalScreen.kt
```

## Publicação

O projeto gera um APK `debug` instalável. Para Google Play, crie uma chave de
assinatura própria, configure o build `release`, revise a política de privacidade
e gere um Android App Bundle assinado pelo Android Studio.
