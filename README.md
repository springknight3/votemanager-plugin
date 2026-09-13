# VoteManager Plugin

Base PaperMC plugin for managing votes on a Minecraft server.

## Build

```sh
mvn package
```

The plugin jar is created in `target/` and can be copied to the server's `plugins/` directory.

The live configuration file is `plugins/vote-manager/config.yml`. The server
process must own that directory and have permission to read and write the file.

An item with the redeemable data can be created with:

```text
/give @s stick[custom_data={vote:{redeemable:{},redeem_name:"Vote Voucher",on_redeem:"shards give <user> 1000"}}]
```

When redeemed, the plugin executes `vote.on_redeem` as the console command and
replaces `<user>` with the player name. The successful redemption message
replaces `<redeem_name>` with `vote.redeem_name`, defaulting to `Vote Voucher`
when the tag is absent. One item is removed only when the command succeeds;
otherwise `redeemable_item_error` is sent.

The `vote:redeemable` key is stored as plugin persistent data inside the item's
`custom_data` component. Minecraft does not allow plugins to register arbitrary
item component names, so `vote:redeemable={}` cannot be used directly as an item
component.
