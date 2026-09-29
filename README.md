# Create: Copycat Replace

> Replace a Copycat's material in a single click — no removal step required.

Create: Copycat Replace! is a small quality-of-life addon for **Create**. Hold a Create Wrench in one hand, hold the new material in the other, and right-click a Copycat to replace its current material directly.

It works with Create's native Copycats and adds optional support for every compatible shape from **Create: Copycats+**, including multi-part blocks.

## How to use

<ol>
  <li>Place or find an empty or already decorated Copycat block.</li>
  <li>Hold the <strong>Create Wrench</strong> in one hand.</li>
  <li>Hold the <strong>new block material</strong> in the other hand.</li>
  <li>Right-click the Copycat or the specific part you want to change.</li>
</ol>

<table>
  <tr>
    <td align="center">
      <img src="https://github.com/radaelilucca/mc-copycat-replace/blob/master/docs/gallery/copycat-replace.gif?raw=true" alt="Replacing a standard Copycat material">
    </td>
    <td align="center">
      <img src="https://github.com/radaelilucca/mc-copycat-replace/blob/master/docs/gallery/copycat-replace-multipart.gif?raw=true" alt="Replacing one part of a multi-part Copycat">
    </td>
  </tr>
</table>

- **Hand order:** Either hand can hold the Wrench or the material.
- **Bypass:** Hold **Sneak** while right-clicking to use the Wrench's normal Create interaction, including removing the Copycat block.

### Connected actions

Hold the modifier key while right-clicking to apply a material to connected empty Copycats, replace the clicked material on matching blocks, or remove that material from the connected group. Only face-connected blocks of the same Copycat type are included. Hold the material in your other hand to apply or replace; use only the Wrench to remove material.

<table>
  <tr>
    <td align="center">
      <img src="https://github.com/radaelilucca/mc-copycat-replace/blob/master/docs/gallery/copycat-connected-apply.gif?raw=true" alt="Applying a material to connected Copycats">
    </td>
    <td align="center">
      <img src="https://github.com/radaelilucca/mc-copycat-replace/blob/master/docs/gallery/copycat-connected-replace.gif?raw=true" alt="Replacing connected Copycat materials">
    </td>
  </tr>
  <tr>
    <td align="center">
      <img src="https://github.com/radaelilucca/mc-copycat-replace/blob/master/docs/gallery/copycat-connected-remove.gif?raw=true" alt="Removing materials from connected Copycats">
    </td>
    <td align="center">
      <img src="https://github.com/radaelilucca/mc-copycat-replace/blob/master/docs/gallery/copycat-connected-multipart-replace.gif?raw=true" alt="Replacing connected multi-part Copycats">
    </td>
  </tr>
</table>

- **Keybind:** Hold **Left Alt** by default. You can change **Bulk Action on Connected Copycats** in **Controls**.
- **Block limit:** Configure the maximum from **1 to 256** (default: **64**) in `config/copycat_replace-common.toml`.

## License and credits

Create: Copycat Replace! is available under the MIT License.

Create and Create: Copycats+ belong to their respective authors. This project is an independent compatibility and quality-of-life addon and is not affiliated with or endorsed by those teams.

## AI disclosure

AI tools were used to assist with research, implementation, and documentation during the development of this project.
