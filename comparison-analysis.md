# CFL (CreateFluidLogistics) vs Fluid 2.0.0 Source Code Similarity Analysis Report

---

## 1. Executive Summary

Fluid 2.0.0 and CFL show a systematic isomorphic relationship in fluid-packaging functionality. The evidence is mainly concentrated in three areas:

1. Main fluid-packaging workflow: from scanning fluid inventory, virtual fluid request items, fluid rendering in Redstone Requester and Factory Panel ghost slots, StockKeeper request-list fluid-entry rendering, extracting fluids according to requests, through to generating packages with address and order information.
2. Clipboard address system: `#`-prefixed address parsing, network packet handling, sign-address protection, address writing, and feedback effects.
3. Factory Panel and StockKeeper interfaces: mixin injection around the same Create methods to modify fluid amount setting, display, restocking, and StockKeeper rendering.

This document has been reorganized into “Executive Summary → Evidence Index → Code-Level Evidence → Fluid Packaging and UI Workflow → Git History and Resource Files → JEI Input Workflow → Architectural Comparison → Overall Judgment.” All code comparisons use a left-right parallel format.

---

## 2. Evidence Index and File Correspondence

| No. | Fluid 2.0.0 File | CFL Corresponding File | Strength | Description |
|------|------------------|--------------|------|------|
| 1 | `util/ClipboardAddressUtil.java` | `util/ClipboardAddressUtil.java` | Very High | Clipboard `#` address parsing |
| 2 | `packet/ClipboardSetAddressPacket.java` | `network/ClipboardSetAddressPacket.java` | Very High | Clipboard address network packet |
| 3 | `mixin/InventorySummaryMixin.java` | `mixin/logistics/InventorySummaryMixin.java` | High | Virtual fluid item statistics |
| 4 | `mixin/FactoryPanelBehaviourMixin.java` | `mixin/logistics/FactoryPanelBehaviourMixin.java` | High | Factory Panel behavior extension |
| 5 | `mixin/CanFillerBlockEntityMixin.java` | `mixin/logistics/PackagerBlockEntityMixin.java` | High | Sign address reading |
| 6 | `mixin/FactoryPanelBlockEntityMixin.java` | `mixin/logistics/FactoryPanelBlockEntityMixin.java` | High | Recognition of the adjacent block behind the panel |
| 7 | `goggle/CanFillerGoggleInfo.java` | `goggle/PackagerGoggleInfo.java` | High | Goggle information display |
| 8 | `util/ICanFillerData.java` | `util/IPackagerOverrideData.java` | High | Address attachment interface |
| 9 | `mixin/StockKeeperRequestScreenMixin.java` | `mixin/client/StockKeeperRequestScreenMixin.java` | High | StockKeeper fluid amount rendering |
| 10 | `mixin/FactoryPanelScreenMixin.java` | `mixin/client/FactoryPanelScreenMixin.java` | Medium | Factory Panel tooltip and amount display |
| 11 | `client/FluidAmountHelper.java` | `util/FluidAmountHelper.java` | Medium | Fluid amount formatting |
| 12 | `client/FluidValueBoxRenderer.java` | `mixin/client/ValueBoxRendererMixin.java` | Medium | ValueBox fluid rendering |

### Complete Evidence Groups

| Evidence Range | Category | Strength Range | Description |
|----------|------|----------|------|
| Evidence 1-12 | Code-level comparison | Medium to Very High | Core code similarities in address handling, inventory statistics, Factory Panel, Goggle, StockKeeper, ValueBox, etc. |
| Evidence 13-18 | Fluid packaging and UI workflow | Medium to High | Packaging, unpacking, virtual fluid request items, Redstone Requester and Factory Panel slot rendering |
| Evidence 19-22 | Git history and resource files | Medium to Very High | Commit timeline, model/blockstate files, PNG blobs, FluidSlotRenderer |
| Evidence 23-25 | JEI, renaming, and item skeleton | Medium to High | JEI ghost target, migration from `fluid_packager` to `can_filler`, `PackageItem` skeleton |

---

## 3. Code-Level Detailed Evidence

### Evidence 1: ClipboardAddressUtil

**Severity: Very High**

| Dimension | Fluid 2.0.0 | CFL |
|------|-------------|-----|
| File | `util/ClipboardAddressUtil.java` | `util/ClipboardAddressUtil.java` |
| Method | Helper methods such as `extractFirstAddress` | `extractFirstAddress` |

```java
// Fluid 2.0.0                                          // CFL git HEAD
public static String extractFirstAddress(ItemStack item) { public static String extractFirstAddress(ItemStack item) {
    if (item != null && !item.isEmpty()) {                  if (item == null || item.isEmpty())
        List pages = ClipboardEntry.readAll(item);              return null;
        return pages.isEmpty() ? null                       List<List<ClipboardEntry>> pages =
            : ClipboardAddressUtil.findFirstAddress(pages);     ClipboardEntry.readAll(item);
    }                                                       if (pages.isEmpty())
    return null;                                                return null;
}                                                           for (List<ClipboardEntry> page : pages) {
                                                                for (ClipboardEntry entry : page) {
private static String findFirstAddress(                             String text = entry.text.getString();
        List<List<ClipboardEntry>> pages) {                         if (text != null && text.startsWith("#")
    for (List<ClipboardEntry> page : pages) {                               && text.length() > 1) {
        for (ClipboardEntry entry : page) {                             String candidate =
            String text = entry.text.getString();                           text.substring(1).stripLeading();
            if (!isValidAddress(text)) continue;                        if (!candidate.isBlank())
            return stripAddressPrefix(text);                                return candidate;
        }                                                               }
    }                                                               }
    return null;                                                }
}                                                               return null;
                                                            }
private static boolean isValidAddress(String text) {
    return text != null && text.startsWith("#")
        && !text.substring(1).isBlank();
}

private static String stripAddressPrefix(String text) {
    return text.substring(1).stripLeading();
}
```

Comparison conclusion: Fluid splits CFL’s inline implementation into `findFirstAddress`, `isValidAddress`, and `stripAddressPrefix`, but the conditions and return logic remain: `readAll` → iterate pages and entries → `startsWith("#")` → `substring(1).stripLeading()` → return if non-empty.

---

### Evidence 2: ClipboardSetAddressPacket

**Severity: Very High**

| Dimension | Fluid 2.0.0 | CFL |
|------|-------------|-----|
| File | `packet/ClipboardSetAddressPacket.java` | `network/ClipboardSetAddressPacket.java` |

```java
// Fluid 2.0.0                                          // CFL git HEAD
Player p = context.player();                             public void handle(ServerPlayer player) {
if (!(p instanceof ServerPlayer player) || !player.mayBuild())    if (!player.mayBuild())
    return;                                                         return;

Level level = player.level();                              Level level = player.level();
if (!level.isLoaded(this.pos)) return;                      if (!level.isLoaded(pos)) return;

BlockEntity be = level.getBlockEntity(this.pos);            if (player.distanceToSqr(pos.getX() + 0.5D,
if (!(be instanceof PackagerBlockEntity packager)) return;          pos.getY() + 0.5D,
                                                                     pos.getZ() + 0.5D) > 64.0D)
if (!ClipboardAddressHandler.isClipboardTarget(                     return;
        level.getBlockState(this.pos))) return;

if (player.distanceToSqr(this.pos.getX() + 0.5,             ItemStack heldItem = player.getMainHandItem();
        this.pos.getY() + 0.5,                              if (!AllBlocks.CLIPBOARD.isIn(heldItem)) return;
        this.pos.getZ() + 0.5) > 64.0)
    return;

ItemStack clipboard =                                      BlockEntity blockEntity = level.getBlockEntity(pos);
    ClipboardAddressHandler.findClipboard(player);          if (!(blockEntity instanceof IPackagerOverrideData data))
if (clipboard == null) return;                                  return;

String resolvedAddress =                                   String address =
    this.address == null ? "" : this.address.strip();           ClipboardAddressUtil.extractFirstAddress(heldItem);
if (resolvedAddress.isEmpty()                              if (address == null) {
        && (resolvedAddress = ClipboardAddressHandler           fluidlogistics$sendFeedback(level, pos, player, false);
            .extractFirstAddress(clipboard)) == null)           return;
    resolvedAddress = "";                                  }

if (resolvedAddress.isEmpty()) {                            if (fluidlogistics$hasSignAddress(level, pos)) {
    PacketDistributor.sendToPlayer(player,                       fluidlogistics$sendFeedback(level, pos, player, false);
        new ClipboardAddressParticlePacket(this.pos));           return;
    return;                                                 }
}

ClipboardSetAddressPacket.applyAddressToPackager(           data.fluidlogistics$setClipboardAddress(address);
    packager, resolvedAddress, level, this.pos, player);     // Then writes into PackagerBlockEntity or FluidPackagerBlockEntity
                                                        }
```

```java
// Fluid 2.0.0                                          // CFL git HEAD
private static void applyAddressToPackager(...) {        if (fluidlogistics$hasSignAddress(level, pos)) {
    if (!(packager instanceof ICanFillerData)) return;        player.displayClientMessage(...);
    ICanFillerData data = (ICanFillerData) packager;          fluidlogistics$sendFeedback(level, pos, player, false);
                                                              return;
    if (checkHasSignAddress(level, pos)) {                }
        player.displayClientMessage(...);
        PacketDistributor.sendToPlayer(player,            data.fluidlogistics$setClipboardAddress(address);
            new ClipboardAddressParticlePacket(pos));     if (blockEntity instanceof PackagerBlockEntity packager) {
        return;                                               packager.signBasedAddress = address;
    }                                                         packager.setChanged();
                                                              packager.notifyUpdate();
    data.setClipboardAddress(address);                    } else if (blockEntity instanceof FluidPackagerBlockEntity fp) {
    packager.signBasedAddress = address;                      fp.signBasedAddress = address;
    packager.notifyUpdate();                                  fp.setChanged();
    PacketDistributor.sendToPlayer(player,                    fp.notifyUpdate();
        new ClipboardAddressParticlePacket(pos));         }
}                                                         fluidlogistics$sendFeedback(level, pos, player, true);
```

Comparison conclusion: Both sides perform permission checks, loaded-chunk checks, a `64.0` distance check, clipboard address extraction, empty-address failure handling, sign-address protection, address writing, and feedback effects. Fluid 2.0.0 centralizes clipboard lookup and parsing in `ClipboardAddressHandler`, while CFL uses held-item checking and `ClipboardAddressUtil` separately.

---

### Evidence 3: InventorySummaryMixin

**Severity: High**

| Dimension | Fluid 2.0.0 | CFL |
|------|-------------|-----|
| File | `mixin/InventorySummaryMixin.java` | `mixin/logistics/InventorySummaryMixin.java` |

```java
// Fluid 2.0.0                                          // CFL git HEAD
private void fluid$addFluidManifest(ItemStack stack,    private void fluidlogistics$addCompressedTank(ItemStack stack,
        int count, CallbackInfo ci) {                           int count, CallbackInfo ci) {
    if (count == 0 || stack.isEmpty()) return;              if (count == 0 || stack.isEmpty()) return;
    if (!(stack.getItem() instanceof FluidManifestItem))     if (!(stack.getItem() instanceof CompressedTankItem))
        return;                                                  return;

    FluidManifestContent content = stack.get(...);           FluidTankContent content = stack.get(...);
    if (content == null || content.fluid().isEmpty())        if (content == null || content.fluid().isEmpty())
        return;                                                  return;

    for (BigItemStack entry : this.stacks) {                 for (BigItemStack entry : this.stacks) {
        if (!matchesSameFluid(entry.stack, stack)) continue;     if (!matchesVirtualFluid(entry.stack, fluid)) continue;
        entry.count = Math.min(1000000000,                       entry.count = Math.min(BigItemStack.INF,
            entry.count + count);                                    entry.count + count);
        ci.cancel();                                             ci.cancel();
        return;                                                  return;
    }                                                        }
    this.stacks.add(new BigItemStack(stack, count));         this.stacks.add(new BigItemStack(stack, count));
    ci.cancel();                                             ci.cancel();
}                                                        }
```

Comparison conclusion: Both use mixins to intercept `InventorySummary`, merge virtual fluid containers as inventory statistic entries, and use the same upper-limit semantics: Fluid writes `1000000000` directly, while CFL uses `BigItemStack.INF`.

---

### Evidence 4: FactoryPanelBehaviourMixin

**Severity: High**

| Dimension | Fluid 2.0.0 | CFL |
|------|-------------|-----|
| File | `mixin/FactoryPanelBehaviourMixin.java` | `mixin/logistics/FactoryPanelBehaviourMixin.java` |

| Injected Method | Fluid | CFL |
|----------|-------|-----|
| `tryRestock` | `@Inject` | `@Inject` |
| `createBoard` | `@Inject` | `@Inject` |
| `formatValue` | `@Inject` | `@Inject` |
| `setValueSettings` | `@Inject` | `@Inject` + `@ModifyExpressionValue` |
| `getValueSettings` | `@Inject` | `@Inject` |
| `getCountLabelForValueBox` | `@Inject` | `@Inject` |

```java
// Fluid 2.0.0                                          // CFL git HEAD
@Inject(method={"tryRestock"}, at=@At("HEAD"),          @Inject(method = "tryRestock", at = @At("HEAD"),
        cancellable=true, remap=false)                          cancellable = true, remap = false)
private void fluid$tryRestockFromCanFiller(CallbackInfo ci) {private void fluidlogistics$tryFluidRestock(CallbackInfo ci) {
    FactoryPanelBehaviour behaviour =                           FactoryPanelBehaviour self =
        (FactoryPanelBehaviour) this;                                (FactoryPanelBehaviour) (Object) this;
    ItemStack item = fluid$normalizeManifest(                    IFluidPackager fluidPackager =
        behaviour.getFilter());                                      FluidGaugeHelper.getFluidPackager(self.panelBE());
    if (!(item.getItem() instanceof FluidManifestItem)) return;   if (fluidPackager == null) return;
    PackagerBlockEntity packager = behaviour.panelBE()            ItemStack item = self.getFilter();
        .getRestockedPackager();
    if (!(packager instanceof CanFillerBlockEntity)) return;

    int inStorage = behaviour.getLevelInStorage();                int inStorage = self.getLevelInStorage();
    int promised = behaviour.getPromised();                       int promised = self.getPromised();
    int demand = behaviour.getAmount();                           int demand = FluidGaugeHelper.getRestockDemand(self);
    int amountToOrder =                                           int shortage = demand - promised - inStorage;
        Math.max(demand - promised - inStorage, 0);

    int availableOnNetwork = LogisticsManager.getStockOf(         IdentifiedInventory inv =
        behaviour.network, item, null);                               fluidPackager.getIdentifiedInventory();
                                                                  int availableOnNetwork =
                                                                      LogisticsManager.getStockOf(network, item, inv);
                                                                  int amountToOrder = Math.min(shortage, availableOnNetwork);

    BigItemStack orderedItem =                                    BigItemStack orderedItem =
        new BigItemStack(item, Math.min(amountToOrder,                new BigItemStack(item, amountToOrder);
            availableOnNetwork));
    PackageOrderWithCrafts order =                                PackageOrderWithCrafts order =
        PackageOrderWithCrafts.simple(List.of(orderedItem));          PackageOrderWithCrafts.simple(List.of(orderedItem));

    LogisticsManager.broadcastPackageRequest(                     LogisticsManager.broadcastPackageRequest(
        behaviour.network, RequestType.RESTOCK, order,                network, RequestType.RESTOCK, order, inv, recipeAddress);
        null, behaviour.recipeAddress);
    behaviour.restockerPromises.add(new RequestPromise(orderedItem));restockerPromises.add(new RequestPromise(orderedItem));
    ci.cancel();                                                  ci.cancel();
}                                                             }
```

```java
// Fluid 2.0.0                                          // CFL git HEAD
@Inject(method={"createBoard"}, at=@At("HEAD"),          @Inject(method = "createBoard", at = @At("RETURN"),
        cancellable=true, remap=false)                          cancellable = true, remap = false)
fluid$createBucketBoard(...) {                           fluidlogistics$modifyBoardLabels(...) {
    new ValueSettingsBoard(                                   new ValueSettingsBoard(
        translate("factory_panel.target_amount"),                 translate("factory_panel.target_amount"),
        100, 10,                                                  100, 10,
        List.of(literal("mB"), literal("B")),                     List.of(text("mB"), text("B")),
        formatter -> behaviour.formatValue(formatter));           original.formatter());
}                                                           }

@Inject(method={"formatValue"}, at=@At("HEAD"),            @Inject(method = "formatValue", at = @At("HEAD"),
        cancellable=true, remap=false)                             cancellable = true, remap = false)
fluid$formatValue(value, cir) {                            fluidlogistics$formatFluidValue(value, cir) {
    if (value.value() == 0) inactive;                         String formatted =
    else if (value.row() == 1) value.value() + "B";               FluidAmountHelper.formatFactoryGaugeValueSetting(
    else value.value() * 10 + "mB";                                   value.row(), value.value());
}                                                               formatted == null ? inactive : literal(formatted);
                                                            }

@Inject(method={"setValueSettings"}, at=@At("HEAD"),       @Inject(method = "setValueSettings", at = @At("HEAD"))
        cancellable=true, remap=false)                     fluidlogistics$beforeSetValueSettings(..., settings, ...) {
fluid$setBucketValueSettings(..., settings, ..., ci) {         needsConversion = true;
    int storedAmountMb = fluid$boardValueToMb(settings);       useBucketsMode = settings.row() == 1;
    behaviour.count = storedAmountMb;                      }
    behaviour.upTo = true;                                 @ModifyExpressionValue(method = "setValueSettings", ...)
    behaviour.blockEntity.sendData();                      fluidlogistics$modifySettingsValue(original) {
    ci.cancel();                                               return FluidAmountHelper.toFactoryGaugeAmount(row, original);
}                                                           }

@Inject(method={"getValueSettings"}, at=@At("HEAD"),       @Inject(method = "getValueSettings", at = @At("HEAD"),
        cancellable=true, remap=false)                             cancellable = true, remap = false)
fluid$getBucketValueSettings(cir) {                        fluidlogistics$onGetValueSettings(cir) {
    cir.setReturnValue(fluid$mbToBoardSettings(count));         cir.setReturnValue(new ValueSettings(row, displayValue));
}                                                           }

@Inject(method={"getCountLabelForValueBox"}, at=@At("HEAD"),@Inject(method = "getCountLabelForValueBox", at = @At("HEAD"),
        cancellable=true, remap=false)                             cancellable = true, remap = false)
fluid$getBucketCountLabel(cir) {                           fluidlogistics$onGetCountLabelForValueBox(cir) {
    text(inStorageText).add("/").add(targetText);               text(format(levelInStorage)).add("/").add(format(count));
}                                                           }
```

Comparison conclusion: Both extend fluid restocking, amount settings, and display around the same `FactoryPanelBehaviour`. In `tryRestock`, the `inStorage/promised/demand` calculation, broadcasting of `PackageOrderWithCrafts`, and writing into `restockerPromises` correspond clearly.

---

### Evidence 5: Sign Address Reading

**Severity: High**

| Dimension | Fluid 2.0.0 | CFL |
|------|-------------|-----|
| File | `mixin/CanFillerBlockEntityMixin.java` | `mixin/logistics/PackagerBlockEntityMixin.java` |

```java
// Fluid 2.0.0                                          // CFL git HEAD
BlockEntity object = level.getBlockEntity(               BlockEntity blockEntity = level.getBlockEntity(
    packager.getBlockPos().relative(side));                   packager.getBlockPos().relative(side));
if (!(object instanceof SignBlockEntity))                 if (!(blockEntity instanceof SignBlockEntity sign))
    return "";                                                return "";
SignBlockEntity sign = (SignBlockEntity) object;

for (boolean front : new boolean[]{true, false}) {        for (boolean front : Iterate.trueAndFalse) {
    SignText text = sign.getText(front);                      SignText text = sign.getText(front);
    String address = "";                                      StringBuilder address = new StringBuilder();
    for (Component component : text.getMessages(false)) {     for (Component component : text.getMessages(false)) {
        String string = component.getString();                    String string = component.getString();
        if (string.isBlank()) continue;                          if (!string.isBlank())
        address += string.trim() + " ";                              address.append(string.trim()).append(' ');
    }                                                           }
    if (address.isBlank()) continue;                            if (address.length() > 0)
    return address.trim();                                          return address.toString().trim();
}                                                           }
return "";                                                  return "";
```

Comparison conclusion: Both read sign addresses in the same sequence: adjacent block → `SignBlockEntity` → front/back side → text components → trim and concatenate → return address.

---

### Evidence 6: FactoryPanelBlockEntityMixin

**Severity: High**

| Dimension | Fluid 2.0.0 | CFL |
|------|-------------|-----|
| File | `mixin/FactoryPanelBlockEntityMixin.java` | `mixin/logistics/FactoryPanelBlockEntityMixin.java` |

```java
// Fluid 2.0.0                                          // CFL git HEAD
@Inject(method={"getRestockedPackager"},                 private boolean fluidlogistics$modifyRestockerCheck(
        at=@At("HEAD"), cancellable=true, remap=false)           boolean original) {
private void fluid$getCanFillerAsRestockedPackager(cir) {    if (original) return true;
    FactoryPanelBlockEntity be =                             FactoryPanelBlockEntity self =
        (FactoryPanelBlockEntity) this;                           (FactoryPanelBlockEntity) (Object) this;
    if (!fluid$isFactoryGauge(be)) return;                    if (self.getLevel() == null) return false;

    BlockEntity attached = fluid$getAttachedBlockEntity(be);  BlockState state = self.getBlockState();
    if (!(attached instanceof CanFillerBlockEntity)) return;  BlockPos connectedPos = self.getBlockPos().relative(
    cir.setReturnValue((CanFillerBlockEntity) attached);          FactoryPanelBlock.connectedDirection(state).getOpposite());
}                                                           if (!self.getLevel().isLoaded(connectedPos)) return false;

private static BlockEntity fluid$getAttachedBlockEntity(     BlockEntity be = self.getLevel().getBlockEntity(connectedPos);
        FactoryPanelBlockEntity be) {                        return be instanceof IFluidPackager;
    BlockState state = be.getBlockState();                }
    BlockPos attachedPos = be.getBlockPos().relative(
        FactoryPanelBlock.connectedDirection(state).getOpposite());
    if (!be.getLevel().isLoaded(attachedPos)) return null;
    return be.getLevel().getBlockEntity(attachedPos);
}
```

Comparison conclusion: Both use `FactoryPanelBlock.connectedDirection(state).getOpposite()` to calculate the adjacent block behind the panel, and then use that block entity to decide whether to enter fluid-packager-related logic.

---

### Evidence 7: Goggle Information Display

**Severity: High**

| Dimension | Fluid 2.0.0 | CFL |
|------|-------------|-----|
| File | `goggle/CanFillerGoggleInfo.java` | `goggle/PackagerGoggleInfo.java` |

```java
// Fluid 2.0.0                                          // CFL git HEAD
translate("goggles.repackager_title")                   translate("goggles.repackager_title")
    .style(ChatFormatting.WHITE).forGoggles(tooltip);        .style(ChatFormatting.WHITE).forGoggles(tooltip);
translate("goggles.address_label")                      translate("goggles.address_label")
    .style(ChatFormatting.GRAY).forGoggles(tooltip, 1);      .style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
text(address)                                           text(address)
    .style(ChatFormatting.GOLD).forGoggles(tooltip, 1);      .style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
translate("goggles.packager_title")                     translate("goggles.packager_title")
    .style(ChatFormatting.WHITE).forGoggles(tooltip);        .style(ChatFormatting.WHITE).forGoggles(tooltip);
translate("goggles.no_address")                         translate("goggles.no_address")
    .style(ChatFormatting.DARK_GRAY).forGoggles(tooltip, 1); .style(ChatFormatting.DARK_GRAY).forGoggles(tooltip, 1);
```

Comparison conclusion: The translation keys, color combinations, and `forGoggles(..., 1)` indentation pattern are consistent.

---

### Evidence 8: Address Attachment Interface

**Severity: High**

| Dimension | Fluid 2.0.0 | CFL |
|------|-------------|-----|
| File | `util/ICanFillerData.java` | `util/IPackagerOverrideData.java` |

```java
// Fluid 2.0.0                                          // CFL git HEAD
public interface ICanFillerData {                       public interface IPackagerOverrideData {
    String getClipboardAddress();                            boolean fluidlogistics$isManualOverrideLocked();
    void setClipboardAddress(String address);                 void fluidlogistics$setManualOverrideLocked(boolean locked);
}                                                            String fluidlogistics$getClipboardAddress();
                                                             void fluidlogistics$setClipboardAddress(String address);
                                                         }
```

Comparison conclusion: Fluid retains the two core address read/write interface methods, while CFL additionally includes manual override lock state.

---

### Evidence 9: StockKeeperRequestScreenMixin

**Severity: High**

| Dimension | Fluid 2.0.0 | CFL |
|------|-------------|-----|
| File | `mixin/StockKeeperRequestScreenMixin.java` | `mixin/client/StockKeeperRequestScreenMixin.java` |

```java
// Fluid 2.0.0                                          // CFL git HEAD
@Inject(method={"renderItemEntry(...)"}, at=@At("HEAD"))@Inject(method = "renderItemEntry", at = @At("HEAD"),
fluid$onRenderItemEntryHead(graphics, pt, entry) {              remap = false, cancellable = true)
    fluid$isFluidManifest = false;                      fluidlogistics$onRenderItemEntryHead(graphics, scale, entry) {
    fluid$fluidAmount = 0;                                  fluidlogistics$isCompressedTank = false;
    fluid$cachedFluid = FluidStack.EMPTY;
    fluid$cachedGraphics = graphics;                        ItemStack stack = entry.stack;
                                                            if (stack.getItem() instanceof CompressedTankItem
    if (entry.stack.getItem() instanceof FluidManifestItem       && CompressedTankItem.isVirtual(stack)) {
            && !FluidManifestItem.read(entry.stack).isEmpty()) { FluidStack fluid = CompressedTankItem.getFluid(stack);
        fluid$isFluidManifest = true;                          if (!fluid.isEmpty())
        fluid$fluidAmount = entry.count;                           fluidlogistics$isCompressedTank = true;
        fluid$cachedFluid = fluid;                             }
    }                                                      }
}

@Redirect(method={"renderItemEntry"},                    @Redirect(method = "renderItemEntry",
        target="StockKeeperRequestScreen.drawItemCount")          target="StockKeeperRequestScreen.drawItemCount")
fluid$redirectDrawItemCount(instance, graphics, count, custom) {fluidlogistics$redirectDrawItemCount(instance, graphics, count, custom) {
    if (fluid$isFluidManifest) {                            if (fluidlogistics$isCompressedTank)
        if (fluid$fluidAmount > 1)                              return;
            FluidSlotAmountRenderer.renderInStockKeeper(    drawItemCount(graphics, count, custom);
                graphics, fluid$fluidAmount);           }
        return;
    }                                                      @Redirect(method = "renderItemEntry",
    callDrawItemCount(graphics, count, custom);                    target="GuiGraphics.renderItemDecorations")
}                                                         fluidlogistics$redirectRenderItemDecorations(..., customCount) {
                                                              if (fluidlogistics$isVirtualCompressedTank(stack)
                                                                      && customCount > 0)
                                                                  FluidSlotAmountRenderer.renderInStockKeeper(graphics, customCount);
                                                          }
```

Comparison conclusion: Both identify virtual fluid entries at the beginning of `renderItemEntry` and intercept `drawItemCount` to take over StockKeeper amount display. Fluid uses `FluidManifestItem`; CFL uses a virtual `CompressedTankItem`; both ultimately call `FluidSlotAmountRenderer.renderInStockKeeper`.

---

### Evidence 10: FactoryPanelScreenMixin

**Severity: Medium**

| Dimension | Fluid 2.0.0 | CFL |
|------|-------------|-----|
| File | `mixin/FactoryPanelScreenMixin.java` | `mixin/client/FactoryPanelScreenMixin.java` |

```java
// Fluid 2.0.0                                          // CFL git HEAD
@Redirect(method={"renderInputItem"},                    @Redirect(method = "renderInputItem",
        target="GuiGraphics.renderComponentTooltip")             target="GuiGraphics.renderComponentTooltip")
fluid$renderInputFluidTooltip(..., BigItemStack item) {  fluidlogistics$redirectInputTooltip(..., BigItemStack item) {
    if (restocker) {                                         if (restocker) {
        translate("gui.factory_panel.sending_item", fluid);      translate("gui.factory_panel.sending_item", fluid);
        translate("gui.factory_panel.sending_item_tip");         translate("gui.factory_panel.sending_item_tip");
        translate("gui.factory_panel.sending_item_tip_1");       translate("gui.factory_panel.sending_item_tip_1");
    } else {                                                } else {
        translate("gui.factory_panel.sending_item",             translate("gui.factory_panel.sending_item",
            fluid + " x" + amountText);                             fluid + " x" + amountText);
        translate("gui.factory_panel.scroll_to_change_amount"); translate("gui.factory_panel.scroll_to_change_amount");
        translate("gui.factory_panel.left_click_disconnect");   translate("gui.factory_panel.left_click_disconnect");
    }                                                        }
    graphics.renderComponentTooltip(font, newTooltips, x, y); graphics.renderComponentTooltip(font, newTooltips, x, y);
}                                                        }
```

Comparison conclusion: Both intercept tooltip rendering in `renderInputItem` and reuse the same Create translation keys plus color/italic style combinations.

---

### Evidence 11: FluidAmountHelper

**Severity: Medium**

| Dimension | Fluid 2.0.0 | CFL |
|------|-------------|-----|
| File | `client/FluidAmountHelper.java` | `util/FluidAmountHelper.java` |
| Topic | mB/B amount formatting | mB/B/KB amount formatting and Factory Panel value conversion |

```java
// Fluid 2.0.0                                          // CFL git HEAD
public static String format(int amountMB) {             public static String formatPrecise(int amount) {
    String text = String.valueOf(                           if (amount < MB_PER_BUCKET)
        (float) amountMB / 1000.0f);                            return amount + "mB";
    if (text.endsWith(".0"))                               return BigDecimal.valueOf(amount, 3)
        text = text.substring(0, text.length() - 2);            .stripTrailingZeros()
    return text;                                                .toPlainString() + "B";
}                                                           }

public static String formatWithUnit(int amountMB) {      public static String formatFactoryGaugeValueSetting(
    int clamped = Math.max(0, amountMB);                         int row, int value) {
    if (clamped < 1000)                                     if (value == 0)
        return clamped + "mb";                                  return null;
    if (clamped % 1000 == 0)                                if (row == 1)
        return clamped / 1000 + "b";                            return Math.clamp(value, 1, 100) + "B";
    return FluidAmountHelper.format(clamped) + "b";         return Math.max(0, value) * 10 + "mB";
}                                                           }

                                                            public static int toFactoryGaugeAmount(int row, int value) {
                                                                if (value <= 0) return 0;
                                                                if (row == 1)
                                                                    return Math.clamp(value, 1, 100) * MB_PER_BUCKET;
                                                                return Math.max(0, value) * 10;
                                                            }
```

Comparison conclusion: Both perform display conversion around the same unit system, `1000 mB = 1 B`. Fluid retains simplified `format/formatWithUnit` methods, while CFL expands into StockKeeper, detailed display, and Factory Panel scroll-wheel value conversion. The core rules remain: display small mB values directly, convert whole buckets to B, and show non-whole buckets as decimals with trailing zeros stripped.

---

### Evidence 12: FluidValueBoxRenderer / ValueBoxRendererMixin

**Severity: Medium**

| Dimension | Fluid 2.0.0 | CFL |
|------|-------------|-----|
| File | `client/FluidValueBoxRenderer.java` | `mixin/client/ValueBoxRendererMixin.java` |
| Topic | Replace item rendering with fluid texture rendering in ValueBox | Intercept ValueBoxRenderer and render virtual fluids |

```java
// Fluid 2.0.0                                          // CFL git HEAD
public static void renderFluidIntoValueBox(             @Inject(method = "renderItemIntoValueBox",
        FluidStack stack, PoseStack ms,                         at = @At("HEAD"), cancellable = true)
        MultiBufferSource buffer, int light) {          private static void fluidlogistics$renderFluidIntoValueBox(
    if (stack.isEmpty() || stack.getFluid() == Fluids.EMPTY)     ItemStack filter, PoseStack ms,
        return;                                                  MultiBufferSource buffer, int light,
    FluidStack renderStack = stack.getAmount() == 0              int overlay, CallbackInfo ci) {
        ? stack.copyWithAmount(1) : stack;                   FluidStack fluid = VirtualFluidDisplayHelper.getDisplayFluid(filter);
                                                            if (!fluid.isEmpty()) {
    IClientFluidTypeExtensions clientFluid =                     FluidSlotRenderer.renderFluidInWorld(fluid, ms, buffer, light);
        IClientFluidTypeExtensions.of(renderStack.getFluid());   ci.cancel();
    TextureAtlasSprite sprite = ...;                         }
    int color = clientFluid.getTintColor(renderStack);       }
    if ((color >> 24 & 0xFF) == 0) a = 1.0f;
                                                        @Inject(method = "renderFlatItemIntoValueBox",
    ms.pushPose();                                             at = @At("HEAD"), cancellable = true)
    ms.scale(0.5f, 0.5f, 0.5f);                    private static void fluidlogistics$renderFlatFluidIntoValueBox(...) {
    ms.translate(-0.5f, -0.5f, -0.14f);                     FluidStack fluid = VirtualFluidDisplayHelper.getDisplayFluid(filter);
    VertexConsumer builder =                                if (fluid.isEmpty()) return;
        buffer.getBuffer(RenderType.translucent());         TransformStack.of(ms).translate(...).rotateYDegrees(180);
    putVertex(builder, pose, ...);                          squashedMS.scale(.5f, .5f, 1 / 1024f);
    ms.popPose();                                           FluidSlotRenderer.renderFluidItemIcon(fluid, squashedMS, buffer, itemLight);
}                                                           ci.cancel();
                                                        }
```

Comparison conclusion: Fluid extracts the rendering logic into a standalone `FluidValueBoxRenderer`, while CFL injects into Create’s `ValueBoxRenderer` via mixin. The functional location is the same: when the filter represents a fluid, skip default item rendering and instead display a fluid texture/fluid quad; for fluids with `amount == 0`, use `copyWithAmount(1)` to make them visible.

---

## 4. Fluid Packaging and UI Workflow Evidence


### Evidence 13: Extracting Fluids by Request and Generating Packages

**Severity: High**

| Dimension | Fluid 2.0.0 | CFL |
|------|-------------|-----|
| File | `block/CanFiller/CanFillerBlockEntity.java` | `block/FluidPackager/FluidPackagerBlockEntity.java` |
| Method | `attemptToSendFluid` | `attemptToSendFluidRequest` |

```java
// Fluid 2.0.0                                          // CFL git HEAD
PackagingRequest nextRequest = queuedRequests.get(0);   PackagingRequest nextRequest = queuedRequests.get(0);
FluidRequestKey requestedKey =                          ItemStack requestedStack = nextRequest.item();
    FluidManifestItem.readKey(nextRequest.item());      if (!(requestedStack.getItem() instanceof CompressedTankItem)) {
if (requestedKey == null) {                                 queuedRequests.remove(0);
    queuedRequests.remove(0);                               return;
    return;                                             }
}
int requestedMb = nextRequest.getCount();               FluidStack requestedFluid = CompressedTankItem.getFluid(requestedStack);
FluidStack extracted = executePlan(requestedKey, requestedMb);if (requestedFluid.isEmpty()) {
if (extracted.isEmpty()) {                                  queuedRequests.remove(0);
    queuedRequests.remove(0);                               return;
    return;                                             }
}
                                                        int remainingCount = nextRequest.getCount();
ItemStack fluidPackage = CopperCanItem.create(          int toExtract = Math.min(remainingCount, Config.getFluidPerPackage());
    extracted, CFCommonConfig.getFluidPerPackage());    FluidStack extractedFluid = extractSpecificFluidFromTank(
PackageItem.clearAddress(fluidPackage);                     fluidHandler, requestedFluid, toExtract);
String address = nextRequest.address();                 ItemStack fluidPackage = createFluidPackage(extractedFluid);
if (address != null && !address.isBlank())              PackageItem.clearAddress(fluidPackage);
    PackageItem.addAddress(fluidPackage, address);      if (fixedAddress != null)
                                                            PackageItem.addAddress(fluidPackage, fixedAddress);
PackageItem.setOrder(fluidPackage,                      PackageItem.setOrder(fluidPackage,
    nextRequest.orderId(), nextRequest.linkIndex(),         fixedOrderId, linkIndexInOrder,
    nextRequest.finalLink().booleanValue(),                 finalLinkInOrder,
    nextRequest.packageCounter().getAndIncrement(),         packageIndexAtLink, finalPackageAtLink,
    nextRequest.isEmpty(), nextRequest.context());          orderContext);
nextRequest.subtract(extracted.getAmount());            nextRequest.subtract(extractedFluid.getAmount());

if (!heldBox.isEmpty() || animationTicks != 0) {        if (!heldBox.isEmpty() || animationTicks != 0) {
    queuedExitingPackages.add(new BigItemStack(fluidPackage, 1));queuedExitingPackages.add(new BigItemStack(fluidPackage, 1));
    return;                                                 return;
}                                                       }
heldBox = fluidPackage;                                 heldBox = fluidPackage;
animationInward = false;                                animationInward = false;
animationTicks = 20;                                    animationTicks = CYCLE;
triggerStockCheck();                                    triggerStockCheck();
notifyUpdate();                                         notifyUpdate();
```

Comparison conclusion: This is not the originally removed arrival-submission logic, but the actual fluid-packaging workflow. Both sides take the target fluid from `PackagingRequest`, extract fluids according to the requested amount, generate a fluid package, clear and write the address, write order metadata, subtract the request amount, and then decide—based on `heldBox/animationTicks`—whether to eject immediately or enter `queuedExitingPackages`.

---

### Evidence 14: Refilling the Target Fluid Container After Unpacking

**Severity: High**

| Dimension | Fluid 2.0.0 | CFL |
|------|-------------|-----|
| File | `block/CanFiller/CanFillerBlockEntity.java` | `block/FluidPackager/FluidPackagerBlockEntity.java` |
| Method | `unwrapCopperCan` | `unwrapBox` + `tick` pending insert |

```java
// Fluid 2.0.0                                          // CFL git HEAD
public boolean unwrapBox(ItemStack box, boolean simulate) {public boolean unwrapBox(ItemStack box, boolean simulate) {
    return CopperCanItem.isCopperCan(box)                    if (animationTicks > 0)
        && unwrapCopperCan(box, simulate);                       return false;
}                                                           if (!FluidPackageItem.isFluidPackage(box))
                                                                return false;
private boolean unwrapCopperCan(ItemStack box, boolean simulate) {
    if (animationTicks > 0) return false;                   IFluidHandler fluidHandler = fluidTarget.getInventory();
    IFluidHandler fluidHandler = getFluidHandler();         if (fluidHandler == null) return false;
    if (fluidHandler == null) return false;
                                                            List<FluidStack> packageFluids = collectPackageFluids(items);
    FluidStack fluid = CopperCanItem.getFluid(box);         if (!packageFluids.isEmpty()
    if (fluid.isEmpty()) return true;                           && !FluidInsertionHelper.canAcceptAll(targetBE, fluidHandler, packageFluids))
    int filled = fluidHandler.fill(fluid, SIMULATE);            return false;
    if (filled < fluid.getAmount()) return false;
    if (simulate) return true;                              if (simulate) return true;

    fluidHandler.fill(fluid, EXECUTE);                      pendingFluidsToInsert.clear();
    previouslyUnwrapped = box.copyWithCount(1);             pendingFluidsToInsert.addAll(packageFluids);
    animationInward = true;                                 previouslyUnwrapped = box.copyWithCount(1);
    animationTicks = 20;                                    animationInward = true;
    triggerStockCheck();                                    animationTicks = CYCLE;
    notifyUpdate();                                         notifyUpdate();
    return true;                                            return true;
}                                                       }

                                                        if (animationTicks == 0 && animationInward
                                                                && !pendingFluidsToInsert.isEmpty()) {
                                                            for (FluidStack fluid : pendingFluidsToInsert)
                                                                fluidHandler.fill(fluid.copy(), EXECUTE);
                                                            pendingFluidsToInsert.clear();
                                                            triggerStockCheck();
                                                        }
```

Comparison conclusion: Fluid directly simulates and executes `fluidHandler.fill` for a single `CopperCanItem`; CFL first collects `FluidStack` values from multiple compressed tanks inside the package, then performs filling through `pendingFluidsToInsert` after the animation ends. The implementation granularity differs, but the state machine is consistent: unpacking check, simulated capacity, recording `previouslyUnwrapped`, inward animation, stock refresh, and synchronization.

---

### Evidence 15: Data Structure of Virtual Fluid Request Items

**Severity: Medium**

| Dimension | Fluid 2.0.0 | CFL |
|------|-------------|-----|
| File | `item/FluidManifestItem.java` | `item/CompressedTankItem.java` |
| Topic | Uses a lightweight item to represent a logistics request for “a certain fluid” | Uses a virtual compressed tank to represent a logistics request for “a certain fluid” |

```java
// Fluid 2.0.0                                          // CFL git HEAD
public static ItemStack of(FluidStack fluid, int amount) {public static void setFluidVirtual(ItemStack stack, FluidStack fluid) {
    ItemStack stack = new ItemStack(CFItems.FLUID_MANIFEST.get());stack.set(AllDataComponents.FLUID_TANK_CONTENT,
    stack.set(CFDataComponents.FLUID_MANIFEST.get(),              new FluidTankContent(fluid.copy(), true));
        new FluidManifestContent(                             }
            BuiltInRegistries.FLUID.getKey(fluid.getFluid()),
            fluid.isEmpty() ? 0 : 1));                    public static boolean isVirtual(ItemStack stack) {
    return stack;                                             FluidTankContent content = stack.get(AllDataComponents.FLUID_TANK_CONTENT);
}                                                           return content != null && content.virtual();
                                                            }
public static FluidStack read(ItemStack stack) {
    FluidManifestContent content = stack.get(...);       public static FluidStack getFluid(ItemStack stack) {
    if (content == null || content.fluidId() == null)        FluidTankContent content = stack.get(AllDataComponents.FLUID_TANK_CONTENT);
        return FluidStack.EMPTY;                            return content != null ? content.fluid() : FluidStack.EMPTY;
    Fluid fluid = BuiltInRegistries.FLUID.get(content.fluidId());}
    return fluid == null ? FluidStack.EMPTY : new FluidStack(fluid, 1);
}

public static FluidRequestKey readKey(ItemStack stack) {private ItemStack createFluidDisplayItem(FluidStack fluid) {
    FluidManifestContent content = stack.get(...);           ItemStack tankStack = new ItemStack(AllItems.COMPRESSED_STORAGE_TANK.get());
    if (content == null || content.fluidId() == null)        CompressedTankItem.setFluidVirtual(tankStack, fluid.copyWithAmount(1));
        return null;                                        return tankStack;
    return new FluidRequestKey(content.fluidId());       }
}
```

Comparison conclusion: Fluid uses `FluidManifestItem` to store `fluidId`, while CFL uses a `CompressedTankItem` with `virtual=true` to store a `FluidStack`. Neither is an actual container inventory; both are “fluid request/display placeholder items” in the logistics system, and both use `copyWithAmount(1)` or an equivalent one-unit fluid representation to indicate the fluid type.

---

### Evidence 16: Fluid Rendering Inside Redstone Requester Slots

**Severity: High**

| Dimension | Fluid 2.0.0 | CFL Old Version |
| --- | --- | --- |
| File | `mixin/client/RedstoneRequesterScreenMixin.java` | `mixin/client/RedstoneRequesterScreenMixin.java` |
| Evidence Version | `F:\mcmod\fluid\fluid-2.0.0-decompiled` | `36e9bd6` (initial commit) |
| Topic | Render fluid icons in Redstone Requester ghost slots | Render fluid icons from virtual compressed tanks in Redstone Requester ghost slots |

**Code Comparison:**

```java
// Fluid 2.0.0
protected void renderSlot(GuiGraphics graphics, Slot slot) {
    FluidStack fluid;
    ItemStack ghostStack;
    SlotItemHandler handlerSlot;
    int slotIndex;
    if (slot instanceof SlotItemHandler
            && (slotIndex = (handlerSlot = (SlotItemHandler)slot).getSlotIndex()) >= 0
            && slotIndex < ((RedstoneRequesterMenu)this.menu).ghostInventory.getSlots()
            && (ghostStack = ((RedstoneRequesterMenu)this.menu).ghostInventory.getStackInSlot(slotIndex))
                    .getItem() instanceof FluidManifestItem
            && !(fluid = FluidManifestItem.read(ghostStack)).isEmpty()) {
        FluidSlotRenderer.renderFluidSlot(graphics, slot.x, slot.y, fluid);
        return;
    }
    super.renderSlot(graphics, slot);
}
```

```java
// CFL old version 36e9bd6
@Override
protected void renderSlot(GuiGraphics graphics, Slot slot) {
    if (slot instanceof SlotItemHandler) {
        int slotIndex = slot.getSlotIndex();
        ItemStack itemStack = menu.ghostInventory.getStackInSlot(slotIndex);
        if (itemStack.getItem() instanceof CompressedTankItem
                && CompressedTankItem.isVirtual(itemStack)) {
            FluidStack fluid = CompressedTankItem.getFluid(itemStack);
            if (!fluid.isEmpty()) {
                FluidSlotRenderer.renderFluidSlot(graphics, slot.x, slot.y, fluid);
                return;
            }
        }
    }
    super.renderSlot(graphics, slot);
}
```

**Similarities:** Both sides override the same `renderSlot(GuiGraphics, Slot)` rendering entry point, first check whether the slot is a `SlotItemHandler`, then read `menu.ghostInventory` by the same slot index, parse the fluid from the ghost item, call the same `FluidSlotRenderer.renderFluidSlot(graphics, slot.x, slot.y, fluid)` when non-empty and return early; otherwise they fall back to `super.renderSlot(...)`. Fluid 2.0.0 replaces CFL old version’s `CompressedTankItem + isVirtual + getFluid` with `FluidManifestItem + read`, but the slot check, ghost-inventory access, non-empty fluid check, and rendering call chain remain essentially identical.

### Evidence 17: Fluid Rendering Inside Slots of the Factory Panel Item-Setting Screen

**Severity: High**

| Dimension | Fluid 2.0.0 | CFL Old Version |
| --- | --- | --- |
| File | `mixin/client/FactoryPanelSetItemScreenMixin.java` | `mixin/client/FactoryPanelSetItemScreenMixin.java` |
| Evidence Version | `F:\mcmod\fluid\fluid-2.0.0-decompiled` | `36e9bd6` (initial commit) |
| Topic | Render fluid icons in Factory Panel item-setting ghost slots | Render virtual-fluid icons in Factory Panel item-setting ghost slots |

**Code Comparison:**

```java
// Fluid 2.0.0
protected void renderSlot(GuiGraphics graphics, Slot slot) {
    FluidStack fluid;
    ItemStack ghostStack;
    SlotItemHandler handlerSlot;
    int slotIndex;
    if (slot instanceof SlotItemHandler
            && (slotIndex = (handlerSlot = (SlotItemHandler)slot).getSlotIndex()) >= 0
            && slotIndex < ((FactoryPanelSetItemMenu)this.menu).ghostInventory.getSlots()
            && (ghostStack = ((FactoryPanelSetItemMenu)this.menu).ghostInventory.getStackInSlot(slotIndex))
                    .getItem() instanceof FluidManifestItem
            && !(fluid = FluidManifestItem.read(ghostStack)).isEmpty()) {
        FluidSlotRenderer.renderFluidSlot(graphics, slot.x, slot.y, fluid);
        return;
    }
    super.renderSlot(graphics, slot);
}
```

```java
// CFL old version 36e9bd6
@Override
protected void renderSlot(GuiGraphics graphics, Slot slot) {
    if (slot instanceof SlotItemHandler) {
        int slotIndex = slot.getSlotIndex();
        ItemStack itemStack = menu.ghostInventory.getStackInSlot(slotIndex);
        if (itemStack.getItem() instanceof CompressedTankItem
                && CompressedTankItem.isVirtual(itemStack)) {
            FluidStack fluid = CompressedTankItem.getFluid(itemStack);
            if (!fluid.isEmpty()) {
                FluidSlotRenderer.renderFluidSlot(graphics, slot.x, slot.y, fluid);
                return;
            }
        }
    }
    super.renderSlot(graphics, slot);
}
```

**Similarities:** Both sides implement the same purpose in the same `FactoryPanelSetItemScreen` mixin: displaying fluid icons in the ghost slots used to set filter items. The code path is the same as in the Redstone Requester evidence: `SlotItemHandler -> ghostInventory.getStackInSlot(slotIndex) -> read fluid -> FluidSlotRenderer.renderFluidSlot(...) -> return -> super.renderSlot(...)`. Fluid 2.0.0 only replaces CFL old version’s virtual compressed tank carrier with `FluidManifestItem`; the remaining control flow and rendering target remain highly consistent.
### Evidence 18: Replacing Item Rendering with Fluid Rendering in StockKeeper Request List Entries

**Severity: High**

| Dimension | Fluid 2.0.0 | CFL Old Version |
| --- | --- | --- |
| File | `mixin/StockKeeperRequestScreenMixin.java` | `mixin/client/StockKeeperRequestScreenMixin.java` |
| Evidence Version | `F:\mcmod\fluid\fluid-2.0.0-decompiled` | `36e9bd6` (initial commit) |
| Topic | Redirect `GuiGameElement.of(ItemStack)` in `renderItemEntry` to replace item icons with fluid icons | Use fluid from virtual compressed tanks to replace item icons in the same rendering entry point |

**Code Comparison:**

```java
// Fluid 2.0.0
@Redirect(
    method = {"renderItemEntry"},
    at = @At(
        value = "INVOKE",
        target = "Lnet/createmod/catnip/gui/element/GuiGameElement;of(Lnet/minecraft/world/item/ItemStack;)Lnet/createmod/catnip/gui/element/GuiGameElement$GuiRenderBuilder;",
        remap = false
    ),
    remap = false
)
private GuiGameElement.GuiRenderBuilder fluid$redirectGuiGameElementOf(ItemStack itemStack) {
    if (this.fluid$isFluidManifest && !this.fluid$cachedFluid.isEmpty()) {
        FluidSlotRenderer.renderFluidSlot(this.fluid$cachedGraphics, 0, 0, this.fluid$cachedFluid);
        return GuiGameElement.of((ItemStack)Blocks.AIR.asItem().getDefaultInstance());
    }
    return GuiGameElement.of((ItemStack)itemStack);
}
```

```java
// CFL old version 36e9bd6
@Redirect(
    method = "renderItemEntry",
    at = @At(
        value = "INVOKE",
        target = "Lnet/createmod/catnip/gui/element/GuiGameElement;of(Lnet/minecraft/world/item/ItemStack;)Lnet/createmod/catnip/gui/element/GuiGameElement$GuiRenderBuilder;",
        remap = false
    ),
    remap = false
)
private GuiGameElement.GuiRenderBuilder fluidlogistics$redirectGuiGameElementOf(
        ItemStack itemStack,
        @Local(argsOnly = true) GuiGraphics graphics) {
    if (fluidlogistics$isCompressedTank && fluidlogistics$cachedFluid != null) {
        FluidSlotRenderer.renderFluidSlot(graphics, 0, 0, fluidlogistics$cachedFluid);
        return GuiGameElement.of(Blocks.AIR.asItem().getDefaultInstance());
    }
    return GuiGameElement.of(itemStack);
}
```

**Similarities:** Both sides intercept exactly the same `GuiGameElement.of(ItemStack)` call inside `StockKeeperRequestScreen.renderItemEntry`. When a fluid entry matches, both first call `FluidSlotRenderer.renderFluidSlot(...)` at `(0, 0)` to draw the fluid, then return `GuiGameElement.of(Blocks.AIR.asItem().getDefaultInstance())` to replace the original item icon; otherwise they fall back to the original `GuiGameElement.of(itemStack)`. Fluid 2.0.0 replaces CFL old version’s `fluidlogistics$isCompressedTank / fluidlogistics$cachedFluid` with `fluid$isFluidManifest / fluid$cachedFluid` and caches `GuiGraphics` in a field, but the interception target, control branch, fluid-rendering call, and AIR placeholder replacement strategy are essentially identical.

## 5. Git History and Resource File Evidence

This section continues evidence collection based on the git histories of the two repositories, focusing on comparing CFL’s initial commit with CreateFluid’s later commits that introduced fluid-packager-related functionality.

### Evidence 19: Key Commit Timeline

**Severity: High**

| Project | Commit | Time | Description |
| --- | --- | --- | --- |
| CFL | `36e9bd6` | `2026-03-01 22:39:31 +0800` | Initial commit, already containing `FluidPackagerBlockEntity`, fluid packages, virtual compressed tanks, fluid slot rendering, Factory Panel / Redstone Requester / StockKeeper mixins, and complete models and textures |
| CreateFluid | `21036c0` | `2026-04-21 16:23:40 +0800` | `Fluid package foundation`, first centralized addition of `FluidPackagerBlockEntity`, `FluidManifestItem`, `FluidPackageItem`, `InventorySummaryMixin`, fluid packager models, etc. |
| CreateFluid | `4020886` | `2026-04-22 23:36:20 +0800` | `Can filler part one`, adds `FluidSlotRenderer`, `FluidSlotAmountRenderer`, `StockKeeperRequestScreenMixin`, and replaces the fluid packager textures |
| CFL | `23075ad` | `2026-03-27 01:15:18 +0800` | Factory Panel compatibility commit predating CreateFluid; already optimized fluid amount display |
| CFL | `37d3b4b` | `2026-04-30 22:45:50 +0800` | Refactored clipboard address setting; CreateFluid’s current working tree contains a similar clipboard address setting file |

Comparison conclusion: CFL already had a complete fluid logistics workflow in its `2026-03-01` initial commit; CreateFluid only introduced highly similar fluid packaging, fluid rendering, and resource files in two later steps on `2026-04-21` and `2026-04-22`. The timeline supports the judgment that the later project reused the earlier implementation.

---

### Evidence 20: Fluid Packager Blockstate and Model Files Are Highly Consistent

**Severity: Very High**

| File | CreateFluid Commit | CFL Commit | Result |
| --- | --- | --- | --- |
| `assets/*/blockstates/fluid_packager.json` | `21036c0` | `36e9bd6` | Completely identical after only the namespace differs; both are 88 lines |
| `assets/*/models/block/fluid_packager/item.json` | `21036c0` | `36e9bd6` | Completely identical after only namespace/texture namespace differs; both are 139 lines |
| `assets/*/models/block/fluid_packager/tray.json` | `21036c0` | `36e9bd6` | Completely identical after only the namespace differs; both are 37 lines |
| `assets/*/models/item/fluid_packager.json` | `21036c0` | `36e9bd6` | Both contain only one `parent` pointing to the corresponding namespace’s `block/fluid_packager/item` |

```json
// CreateFluid 21036c0
"facing=east,linked=false,powered=false": {
  "model": "fluid:block/fluid_packager/block",
  "y": 270
}

// CFL 36e9bd6
"facing=east,linked=false,powered=false": {
  "model": "fluidlogistics:block/fluid_packager/block",
  "y": 270
}
```

```json
// CreateFluid 21036c0
{
  "parent": "fluid:block/fluid_packager/item"
}

// CFL 36e9bd6
{
  "parent": "fluidlogistics:block/fluid_packager/item"
}
```

Comparison conclusion: These JSON resources are not inevitable API products. In particular, the simultaneous consistency of the `facing` combinations, `linked/powered` combinations, rotation angles, model hierarchy, and Blockbench element structure constitutes resource-structure-level replication. CreateFluid later only changed the namespace from `fluidlogistics` to `fluid`, and in some files changed references to Create or Fluid’s own textures.

---

### Evidence 21: Fluid Packager PNG Textures Are the Same Git Blobs

**Severity: Very High**

Comparison targets:

- CreateFluid: `4020886` (`2026-04-22`, `Can filler part one`)
- CFL: `36e9bd6` (`2026-03-01`, initial commit)

| Texture File | Whether the Git blob is identical |
| --- | --- |
| `packager_frame.png` | Identical: `bca68cb8b85fcf9c3753212548c5aa6dbb034561` |
| `packager_details.png` | Identical: `a3d89c98bdd0ae73bebc02d80195daa20d1be5c6` |
| `packager_horizontal_linked.png` | Identical: `abbe8dca2459b40214ae0679c6092039f4e91c28` |
| `packager_horizontal_powered.png` | Identical: `b71b589d030a97ef43c9572a836f407feb4071de` |
| `packager_horizontal_unpowered.png` | Identical: `ed1178d86da4517f008eadd21d7f09375454c8a6` |
| `packager_iris_closed.png` | Identical: `e20213694838ba930136190db37dedd0092417f1` |
| `packager_iris_open.png` | Identical: `f5df469fd094484475d266a8c50333800ec5dd9a` |
| `packager_particle.png` | Identical: `8618c4a527e725f7bf97a7670479daa550a5fb71` |
| `packager_vertical_linked.png` | Identical: `517d7b3c481635aa9aa28c56b9e2f7ab7930808b` |
| `packager_vertical_powered.png` | Identical: `7ce0a1334c6bab45a91f19b4657dfae0b9c05d55` |
| `packager_vertical_unpowered.png` | Identical: `cc5ab6f440111784ffdd9995968cae2b9becee23` |

Comparison conclusion: The Git blobs of the PNG binary files are completely identical, meaning the contents are byte-for-byte identical. This is more direct than code-structure similarity: the fluid packager textures introduced/replaced by CreateFluid in `4020886` are not merely “stylistically similar” to the corresponding textures in CFL’s initial commit; they are the same content.

---

### Evidence 22: FluidSlotRenderer as a Similar Custom Slot Rendering Utility

**Severity: Medium to High**

| Dimension | CreateFluid | CFL |
| --- | --- | --- |
| File | `client/FluidSlotRenderer.java` | `render/FluidSlotRenderer.java` |
| Evidence Version | `4020886` | `36e9bd6` |
| Topic | Render fluid icons in GUI slots | Render fluid icons in GUI slots, with additional world-rendering support |

```java
// CreateFluid 4020886
if (stack.isEmpty() || stack.getFluid() == Fluids.EMPTY) return;
FluidStack renderStack = stack;
if (stack.getAmount() == 0)
    renderStack = stack.copyWithAmount(1);
IClientFluidTypeExtensions clientFluid = IClientFluidTypeExtensions.of(renderStack.getFluid());
TextureAtlasSprite sprite = Minecraft.getInstance()
    .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
    .apply(clientFluid.getStillTexture(renderStack));
int color = clientFluid.getTintColor(renderStack);
float r = ((color >> 16) & 0xFF) / 255.0f;
float g = ((color >> 8) & 0xFF) / 255.0f;
float b = (color & 0xFF) / 255.0f;
float a = ((color >> 24) & 0xFF) / 255.0f;
if (a == 0) a = 1.0f;
graphics.blit(x + 1, y + 1, 2, 14, 14, sprite, r, g, b, a);
```

```java
// CFL 36e9bd6
if (stack.isEmpty() || stack.getFluid() == Fluids.EMPTY) return;
FluidStack renderStack = stack;
if (stack.getAmount() == 0)
    renderStack = stack.copyWithAmount(1);
IClientFluidTypeExtensions clientFluid = IClientFluidTypeExtensions.of(renderStack.getFluid());
TextureAtlasSprite sprite = Minecraft.getInstance()
    .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
    .apply(clientFluid.getStillTexture(renderStack));
int color = clientFluid.getTintColor(renderStack);
float r = ((color >> 16) & 0xFF) / 255.0f;
float g = ((color >> 8) & 0xFF) / 255.0f;
float b = (color & 0xFF) / 255.0f;
float a = ((color >> 24) & 0xFF) / 255.0f;
if (a == 0) a = 1.0f;
graphics.blit(x + 1, y + 1, 2, 14, 14, sprite, r, g, b, a);
```

Create source verification: In `F:\mcmod\Create-mc1.21.1-6.0.9`, no class or method named `FluidSlotRenderer`, `FluidSlotAmountRenderer`, or `renderFluidSlot` was found. Create’s `foundation/fluid/FluidRenderer.java` uses standard NeoForge/Minecraft fluid-rendering APIs such as `getStillTexture` and `getTintColor`, but it does not provide the `14x14` GUI slot icon drawing utility used here.

Comparison conclusion: CreateFluid’s `renderFluidSlot` and CFL’s method of the same name are completely identical in their empty-fluid check, conversion of `amount == 0` to `copyWithAmount(1)`, retrieval of still texture, retrieval of tint color, RGBA decomposition, alpha fallback, and `graphics.blit(x + 1, y + 1, 2, 14, 14, ...)` coordinate parameters. This similarity cannot be fully attributed to a same-named method in Create because Create does not contain this utility. However, reading fluid textures/colors is a common NeoForge rendering process, so this item should be used as medium-to-high supporting evidence together with the Redstone Requester, Factory Panel, and StockKeeper call-site evidence, rather than as standalone strong evidence.

---

### Interim Summary: Git History and Resource Files

The newly added git-history evidence strengthens the original conclusion:

1. CFL’s complete fluid-packager system appeared earlier than the corresponding CreateFluid commits.
2. In later commits, CreateFluid not only replicated code structure, but also introduced fluid packager PNG textures that are byte-for-byte identical to CFL’s.
3. CreateFluid’s blockstate files, model JSON, GUI fluid-rendering utilities, and amount texture drawing logic form a continuous evidence chain with CFL: “resource files + rendering utilities + mixin call sites + logistics behavior.”

---

## 6. JEI Input Workflow Evidence

### Evidence 23: JEI Fluid Ghost Target Has Matching Landing Points in Two Interfaces

**Severity: Medium**

| Dimension | CreateFluid | CFL |
| --- | --- | --- |
| File | `compat/jei/FluidGhostTarget.java`, `mixin/compat/jei/GhostIngredientHandlerMixin.java` | `compat/jei/FactoryPanelSetItemFluidGhostHandler.java`, `compat/jei/RedstoneRequesterFluidGhostHandler.java` |
| Topic | Write JEI fluid ingredients into Create ghostInventory | Write JEI fluid ingredients into Create ghostInventory |

Reason for down-weighting: The basic structure of `slotIndex + 36`, `Rect2i(guiLeft + slot.x, guiTop + slot.y, 16, 16)`, `ghostInventory.setStackInSlot`, and `GhostItemSubmitPacket` comes from Create’s native `GhostIngredientHandler`. Therefore, this portion cannot be treated as strong evidence on its own.

Similarities that can still be retained:

```java
// CreateFluid
if (ingredient.getType() == NeoForgeTypes.FLUID_STACK && gui instanceof FactoryPanelSetItemScreen) { ... }
if (ingredient.getType() == NeoForgeTypes.FLUID_STACK && gui instanceof RedstoneRequesterScreen) { ... }
...
ItemStack manifest = FluidManifestItem.of(fluid, DEFAULT_FLUID_RECIPE_AMOUNT_MB);
gui.getMenu().ghostInventory.setStackInSlot(slotIndex, manifest);
CatnipServices.NETWORK.sendToServer(new GhostItemSubmitPacket(manifest, slotIndex));
```

```java
// CFL
boolean acceptsFluid = ingredient.getType() == NeoForgeTypes.FLUID_STACK;
...
stack = new ItemStack(AllItems.COMPRESSED_STORAGE_TANK.get());
CompressedTankItem.setFluidVirtual(stack, fluidStack.copyWithAmount(1));
gui.getMenu().ghostInventory.setStackInSlot(slotIndex, stack);
CatnipServices.NETWORK.sendToServer(new GhostItemSubmitPacket(stack, slotIndex));
```

Comparison conclusion: Both sides extend `NeoForgeTypes.FLUID_STACK` on top of Create’s native item ghost target, and the targets are not arbitrary GUIs, but the same two logistics interfaces: `FactoryPanelSetItemScreen` and `RedstoneRequesterScreen`. CreateFluid uses `FluidManifestItem`; CFL uses a virtual `CompressedTankItem`; however, the chain “JEI fluid ingredient → virtual fluid request item → ghostInventory → GhostItemSubmitPacket” is consistent. Because the base framework comes from Create, this item should be treated as medium supporting evidence.

---

### Evidence 24: CreateFluid Git History Shows `fluid_packager` Was Renamed Wholesale to `can_filler`

**Severity: High**

Evidence commits:

| Project | Commit | Time | Description |
| --- | --- | --- | --- |
| CreateFluid | `21036c0` | `2026-04-21 16:23:40 +0800` | Added `fluid_packager`, `FluidPackagerBlockEntity`, `FluidPackageItem`, and `FluidPackageContent` |
| CreateFluid | `4020886` | `2026-04-22 23:36:20 +0800` | Added `fluid_packager` textures and rendering/StockKeeper support |
| CreateFluid | `98b47f9` | `2026-04-23 10:23:02 +0800` | Renamed/migrated the entire `fluid_packager` system to `can_filler` / `copper_can` |

Rename similarity reported by `git show --name-status --find-renames --find-copies 98b47f9`:

| Git rename record | Similarity | Meaning |
| --- | --- | --- |
| `block/fluidpackager/FluidPackagerBlock.java` -> `block/canfiller/CanFillerBlock.java` | `R089` | Block class renamed from fluid packager to can filler |
| `block/fluidpackager/FluidPackagerBlockEntity.java` -> `block/canfiller/CanFillerBlockEntity.java` | `R089` | Main block entity migrated with 89% similarity |
| `mixin/PackagerBlockEntityMixin.java` -> `mixin/CanFillerBlockEntityMixin.java` | `R085` | Packager mixin renamed to can filler mixin |
| `util/IPackagerData.java` -> `util/ICanFillerData.java` | `R077` | Address interface renamed |
| `datacomponent/FluidPackageContent.java` -> `datacomponent/CopperCanContent.java` | `R052` | Fluid package content renamed to copper can content |
| `assets/fluid/blockstates/fluid_packager.json` -> `assets/fluid/blockstates/can_filler.json` | `R051` | Blockstate renamed from fluid packager to can filler |
| `models/block/fluid_packager/block.json` -> `models/block/can_filler/block.json` | `R100` | Main model migrated to the new path with complete identity |
| `models/block/fluid_packager/block_vertical.json` -> `models/block/can_filler/block_vertical.json` | `R100` | Vertical model migrated to the new path with complete identity |
| `models/block/fluid_packager/item.json` -> `models/block/can_filler/item.json` | `R100` | Item model migrated to the new path with complete identity |
| `models/block/fluid_packager/tray.json` -> `models/block/can_filler/tray.json` | `R100` | Tray model migrated to the new path with complete identity |

Registration-name changes in the same commit:

```java
// Before 98b47f9
public static final BlockEntry<FluidPackagerBlock> FLUID_PACKAGER = REGISTRATE
        .block("fluid_packager", FluidPackagerBlock::new)

// After 98b47f9
public static final BlockEntry<CanFillerBlock> CAN_FILLER = REGISTRATE
        .block("can_filler", CanFillerBlock::new)
```

```java
// Before 98b47f9
public static final ItemEntry<FluidPackageItem> FLUID_PACKAGE = REGISTRATE
    .item("fluid_package", FluidPackageItem::new)

// After 98b47f9
public static final ItemEntry<CopperCanItem> COPPER_CAN = REGISTRATE
    .item("copper_can", CopperCanItem::new)
```

`FluidPackageContent` and the renamed `CopperCanContent` retain the same data shape:

```java
// Before renaming
public record FluidPackageContent(FluidStack fluid, int capacity) {
    public static final int DEFAULT_CAPACITY = 10000;
    FluidStack.CODEC.fieldOf("fluid")
    Codec.INT.fieldOf("capacity")
}

// After renaming
public record CopperCanContent(FluidStack fluid, int capacity) {
    public static final int DEFAULT_CAPACITY = 10000;
    FluidStack.CODEC.fieldOf("fluid")
    Codec.INT.fieldOf("capacity")
}
```

Comparison conclusion: CreateFluid did not initially appear as an independently designed `CanFiller/CopperCan` system. The git history shows that it first introduced `FluidPackager/FluidPackage` on `2026-04-21`, directly corresponding to CFL’s `FluidPackager/FluidPackage` naming and resource system; only afterward, on `2026-04-23`, was it batch-renamed to `CanFiller/CopperCan`. Multiple model files are `R100`, and the core block entity is `R089`, indicating that this was a wholesale renaming/refactoring of the previous day’s fluid packager implementation rather than an independent implementation from scratch.

---

### Evidence 25: The `FluidPackageItem` Package-Item Skeleton Is Consistent and Was Later Renamed to `CopperCanItem`

**Severity: Medium to High**

| Dimension | CreateFluid | CFL |
| --- | --- | --- |
| File | `item/FluidPackageItem.java` | `item/FluidPackageItem.java` |
| Evidence Version | `21036c0` | `36e9bd6` |
| Later Evolution | `98b47f9` deletes `FluidPackageItem` and adds `CopperCanItem` | Continues to keep `FluidPackageItem` |

```java
// CreateFluid 21036c0
public class FluidPackageItem extends PackageItem {
    public static final PackageStyle FLUID_STYLE =
        new PackageStyles.PackageStyle("rare_creeper", 12, 10, 21f, true);

    public FluidPackageItem(Properties properties) {
        super(properties, FLUID_STYLE);
        PackageStyles.ALL_BOXES.remove(this);
        PackageStyles.RARE_BOXES.remove(this);
    }
}
```

```java
// CFL 36e9bd6
public class FluidPackageItem extends PackageItem {
    public static final PackageStyle FLUID_STYLE =
        new PackageStyle("rare_fluid", 12, 10, 21f, true);

    public FluidPackageItem(Properties properties) {
        super(properties, FLUID_STYLE);
        PackageStyles.ALL_BOXES.remove(this);
        PackageStyles.RARE_BOXES.remove(this);
    }
}
```

CreateFluid migrated this skeleton to `CopperCanItem` in `98b47f9`:

```java
// CreateFluid 98b47f9
public class CopperCanItem extends PackageItem {
    public static final PackageStyle COPPER_CAN_STYLE =
        new PackageStyles.PackageStyle("rare_creeper", 12, 10, 21f, true);

    public CopperCanItem(Properties properties) {
        super(properties, COPPER_CAN_STYLE);
        PackageStyles.ALL_BOXES.remove(this);
        PackageStyles.RARE_BOXES.remove(this);
    }
}
```

Comparison conclusion: Neither side is an ordinary `Item`; both disguise the fluid container as Create’s `PackageItem`, use the same `PackageStyle` parameters `12, 10, 21f, true`, and remove it from `PackageStyles.ALL_BOXES` and `PackageStyles.RARE_BOXES` to prevent it from participating in regular package-style lists. CreateFluid later renamed `FluidPackageItem` to `CopperCanItem`, but retained the same `PackageItem` skeleton and style handling. This evidence is weaker than byte-for-byte textures and `R100` model renames, but it strengthens the historical chain in which the “fluid package object” was later renamed to a copper can.

---

### Interim Summary: JEI and Renaming Workflow

Under the current evidence boundary, only committed history and non-working-tree temporary-file evidence are retained:

1. The basic writing pattern of JEI ghost targets comes from Create and should be down-weighted; however, both sides extend it to the same two logistics interfaces, so it remains usable as combined evidence.
2. Binary resource evidence is further strengthened: in addition to the ten listed packager textures, `packager_details.png` is also the same Git blob as in CFL’s initial commit.
3. CreateFluid’s git history shows that `fluid_packager/fluid_package` was renamed wholesale to `can_filler/copper_can` within two days after being introduced; multiple model files are `R100`, and the core block entity is `R089`.
4. The `PackageItem` skeleton of `FluidPackageItem` matches CFL’s initial commit and was migrated to `CopperCanItem` in `98b47f9`, strengthening the “rename-style refactoring” chain.

---

## 7. Architectural-Level Comparison

| Dimension | Fluid 2.0.0 | CFL |
|------|-------------|-----|
| Core block entity | `CanFillerBlockEntity` extends `PackagerBlockEntity` | `FluidPackagerBlockEntity` is independently implemented |
| Virtual item | `FluidManifestItem` + `FluidManifestContent` | `CompressedTankItem` + `FluidTankContent` |
| Logistics interface | `IFluidLogisticsPackager` | `IFluidPackager` |
| Goggle information | `CanFillerGoggleInfo` | `PackagerGoggleInfo` |
| Mixin prefix | `fluid$` | `fluidlogistics$` |

Fluid 2.0.0’s main replication methods still include:

1. Method-splitting copy: splitting CFL’s inline logic into multiple private helper methods.
2. Rename-style replacement: replacing `CompressedTankItem` with `FluidManifestItem`, and replacing the `fluidlogistics$` prefix with `fluid$`.
3. Inheritance-based alternative implementation: simplifying the structure by extending Create’s `PackagerBlockEntity` while retaining the core logistics workflow.
4. Simplified replication: keeping only the core paths related to CanFiller and Factory Panel interaction.

---

## 8. Overall Judgment

| Strength | Representative Evidence | Summary |
|------|----------|------|
| Very High | Evidence 1, 2, 20, 21 | Clipboard address logic is highly isomorphic; fluid packager blockstate/model JSON structures are consistent; PNG texture Git blobs are byte-for-byte identical |
| High | Evidence 3, 4, 5, 6, 7, 8, 9, 13, 14, 19, 24 | Inventory statistics, Factory Panel, address attachment, Goggle, StockKeeper, packaging/unpacking state machine, timeline, and wholesale rename/migration form a continuous evidence chain |
| Medium to High | Evidence 22, 25 | Custom fluid slot rendering utility and the `FluidPackageItem`/`CopperCanItem` skeleton can serve as supporting evidence |
| Medium | Evidence 10, 11, 12, 15, 16, 17, 18, 23 | Similarities in amount formatting, ValueBox/interface rendering, virtual fluid request items, and JEI input workflow, but some base structures come from Create or NeoForge and should be down-weighted |

Final judgment: In key functions including fluid packaging/unpacking, virtual fluid request items, fluid rendering in Redstone Requester and Factory Panel ghost slots, replacement of fluid entries in the StockKeeper request list, clipboard addresses, Factory Panel restocking, ValueBox fluid rendering, resource files, and the historical renaming chain, Fluid 2.0.0 shows systematic code-structure similarity with CFL. The similarities are not isolated utility methods or UI constants, but a continuous implementation path spanning block entities, network packets, mixin injections, interface rendering, resource files, address interfaces, and git history.

---
